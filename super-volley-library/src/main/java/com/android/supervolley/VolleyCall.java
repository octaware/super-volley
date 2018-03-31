package com.android.supervolley;

import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.RequestFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;

final class VolleyCall<T> implements Call<T> {

    private final ServiceMethod<T, ?> serviceMethod;
    private final Object[] args;
    private final RequestQueue requestQueue;

    private volatile boolean canceled;

    // All guarded by this.
    private BaseRequest rawCall;
    private Throwable creationFailure; // Either a RuntimeException or IOException.
    private boolean executed;
    private int timeOut;

    VolleyCall(ServiceMethod<T, ?> serviceMethod, Object[] args, RequestQueue requestQueue, int timeOut) {
        this.serviceMethod = serviceMethod;
        this.requestQueue = requestQueue;
        this.args = args;
        this.timeOut = timeOut;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    // We are a final type & this saves clearing state.
    @Override
    public Call<T> clone() {
        return new VolleyCall<>(serviceMethod, args, requestQueue, timeOut);
    }

    @Override
    public synchronized Request request() {
        Request call = rawCall;
        if (call != null) {
            return call;
        }
        if (creationFailure != null) {
            if (creationFailure instanceof IOException) {
                throw new RuntimeException("Unable to create request.", creationFailure);
            } else {
                throw (RuntimeException) creationFailure;
            }
        }
        try {
            return (rawCall = createRawCall());
        } catch (RuntimeException e) {
            creationFailure = e;
            throw e;
        } catch (IOException e) {
            creationFailure = e;
            throw new RuntimeException("Unable to create request.", e);
        }
    }

    @Override
    public synchronized boolean isExecuted() {
        return executed;
    }

    @Override
    public void cancel() {
        canceled = true;

        BaseRequest call;
        synchronized (this) {
            call = rawCall;
        }
        if (call != null) {
            call.cancel();
            requestQueue.cancelAll(call.getTag());
        }
    }

    @Override
    public boolean isCanceled() {
        if (canceled) {
            return true;
        }
        synchronized (this) {
            return rawCall != null && rawCall.isCanceled();
        }
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        if (callback == null) throw new NullPointerException("callback == null");

        BaseRequest call;
        Throwable failure;

        synchronized (this) {
            if (executed) throw new IllegalStateException("Already executed.");
            executed = true;

            call = rawCall;
            failure = creationFailure;
            if (call == null && failure == null) {
                try {
                    call = rawCall = createRawCall();
                } catch (Throwable t) {
                    failure = creationFailure = t;
                }
            }
        }

        if (failure != null) {
            callback.onFailure(this, failure);
            return;
        }

        if (canceled) {
            call.cancel();
        }

        final BaseRequest finalCall = call;
        call.setResponseListener(new ResponseListener() {
            @Override
            protected void onSuccess(HttpResponse.Builder builder) {
                int statusCode = finalCall.getStatusCode();
                builder.code(statusCode).request(finalCall);
                try {
                    Response<T> response = parseResponse(builder, statusCode);
                    callSuccess(response);
                } catch (Throwable e) {
                    callFailure(e);
                }
            }

            @Override
            protected void onFailure(VolleyError payload) {
                try {
                    Response<T> response = parseError(payload, finalCall);
                    callSuccess(response);
                } catch (Throwable e) {
                    callFailure(e);
                }
            }

            private void callFailure(Throwable e) {
                try {
                    callback.onFailure(VolleyCall.this, e);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void callSuccess(Response<T> response) {
                try {
                    callback.onResponse(VolleyCall.this, response);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });

        requestQueue.add(call);
    }

    @Override
    public Response<T> execute() throws IOException {
        BaseRequest call;

        synchronized (this) {
            if (executed) throw new IllegalStateException("Already executed.");
            executed = true;

            if (creationFailure != null) {
                if (creationFailure instanceof IOException) {
                    throw (IOException) creationFailure;
                } else {
                    throw (RuntimeException) creationFailure;
                }
            }

            call = rawCall;
            if (call == null) {
                try {
                    call = rawCall = createRawCall();
                } catch (IOException | RuntimeException e) {
                    creationFailure = e;
                    throw e;
                }
            }
        }

        if (canceled) {
            call.cancel();
        }

        RequestFuture<HttpResponse.Builder> future = RequestFuture.newFuture();
        call.setFutureRequest(future);
        requestQueue.add(call);
        try {
            HttpResponse.Builder builder = future.get(timeOut, TimeUnit.SECONDS);
            return parseResponse(builder, call.getStatusCode());
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // exception handling
            if (e.getCause() instanceof VolleyError) {
                return parseError(((VolleyError) e.getCause()), call);
            }
            return Response.error(new HttpResponse.Builder()
                    .message(e.getMessage()).request(call)
                    .success(false).code(-1).build());
        }
    }

    private BaseRequest createRawCall() throws IOException {
        Request request = serviceMethod.toRequest(args);
        if (request == null) {
            throw new NullPointerException("Call.Factory returned null.");
        }
        return (BaseRequest) request;
    }

    private Response<T> parseResponse(HttpResponse.Builder builder, int code) throws IOException {
        if (code < 200 || code >= 300) {
            HttpResponse response = builder.success(false).build();
            // Buffer the entire body to avoid future I/O.
            ResponseBody bufferedBody = Utils.buffer(ResponseBody.create(null, response.toString()));
            return Response.error(bufferedBody, response);
        }

        HttpResponse response = builder.success(true).build();
        if ((code == 204 || code == 205) && (response.toString() == null || response.toString().isEmpty())) {
            return Response.success(null, response);
        }

        ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(
                ResponseBody.create(null, response.toString()));
        try {
            T body = serviceMethod.toResponse(catchingBody);
            return Response.success(body, response);
        } catch (RuntimeException e) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            catchingBody.throwIfCaught();
            throw e;
        }
    }

    private Response<T> parseError(VolleyError error, BaseRequest request) {
        HttpResponse.Builder builder = new HttpResponse.Builder()
                .success(false)
                .request(request)
                .message(error.getMessage());

        if (error instanceof NetworkError || error instanceof TimeoutError) {
            VolleyLog.e("Network error.");
            if (error.getCause() instanceof SSLHandshakeException) {
                VolleyLog.e("SSLHandshakeException: Insecure connection");
            }
            return Response.error(builder.build());
        }

        NetworkResponse response = error.networkResponse;
        if (response == null) {
            VolleyLog.e("Local error.");
            return Response.error(builder.build());
        }

        String json = "";
        if (response.data != null) {
            json = new String(response.data);
        }
        ResponseBody body = ResponseBody.create(MediaType.parse(response.headers.get("Content-Type")), json);
        return Response.error(body, builder.raw(json).code(response.statusCode).build());
    }

    private static final class ExceptionCatchingRequestBody extends ResponseBody {
        private final ResponseBody delegate;
        IOException thrownException;

        ExceptionCatchingRequestBody(ResponseBody content) {
            this.delegate = content;
        }

        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() {
            return delegate.contentLength();
        }

        @Override
        public BufferedSource source() {
            return Okio.buffer(new ForwardingSource(delegate.source()) {
                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    try {
                        return super.read(sink, byteCount);
                    } catch (IOException e) {
                        thrownException = e;
                        throw e;
                    }
                }
            });
        }

        @Override
        public void close() {
            delegate.close();
        }

        void throwIfCaught() throws IOException {
            if (thrownException != null) {
                throw thrownException;
            }
        }
    }
}
