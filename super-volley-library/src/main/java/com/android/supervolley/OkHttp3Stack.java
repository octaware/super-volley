package com.android.supervolley;

import com.android.supervolley.BaseRequest;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;

import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;


class OkHttp3Stack implements OkHttpStack {

    private final okhttp3.Call.Factory client;

    OkHttp3Stack(okhttp3.Call.Factory client) {
        this.client = client;
    }

    @SuppressWarnings("deprecation")
    private void setConnectionParametersForRequest(okhttp3.Request.Builder builder, Request<?> request)
            throws IOException, AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    builder.post(okhttp3.RequestBody.create
                            (MediaType.parse(request.getPostBodyContentType()), postBody));
                } else {
                    builder.get();
                }
                break;

            case Request.Method.GET:
                builder.get();
                break;

            case Request.Method.DELETE:
                builder.delete();
                break;

            case Request.Method.POST:
                builder.post(createRequestBody(request));
                break;

            case Request.Method.PUT:
                builder.put(createRequestBody(request));
                break;

            case Request.Method.HEAD:
                builder.head();
                break;

            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;

            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;

            case Request.Method.PATCH:
                builder.patch(createRequestBody(request));
                break;

            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private okhttp3.RequestBody createRequestBody(Request request) throws AuthFailureError {
        if (request instanceof BaseRequest) {
            return ((BaseRequest) request).getRequestBody();
        }
        final byte[] body = request.getBody();
        if (body == null) {
            // added empty byte array to support empty body
            return okhttp3.RequestBody.create(MediaType.parse(request.getBodyContentType()), new byte[0]);
        }

        return okhttp3.RequestBody.create(MediaType.parse(request.getBodyContentType()), body);
    }

    @Override
    public okhttp3.Response performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();

        Map<String, String> headers = request.getHeaders();
        for (final String name : headers.keySet()) {
            okHttpRequestBuilder.addHeader(name, headers.get(name));
        }

        for (final String name : additionalHeaders.keySet()) {
            okHttpRequestBuilder.addHeader(name, additionalHeaders.get(name));
        }

        setConnectionParametersForRequest(okHttpRequestBuilder, request);

        okhttp3.Request okhttp3Request = okHttpRequestBuilder.url(request.getUrl()).build();

        return client.newCall(okhttp3Request).execute();
    }
}
