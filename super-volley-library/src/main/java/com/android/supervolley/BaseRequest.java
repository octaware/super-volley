package com.android.supervolley;

import com.android.supervolley.HttpResponse;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.RequestBody;

import static com.android.supervolley.InternalHttpHeaderParser.parseIgnoreCacheHeaders;
import static com.android.volley.Response.error;
import static com.android.volley.Response.success;
import static com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders;


/**
 * An abstract request class that defines the basic request build logic for all api calls.
 * This class is made package private to block undesired inheritance.
 */
public class BaseRequest extends StringRequest {

    private String PROTOCOL_CONTENT_TYPE = "application/json; charset=UTF-8";

    // request fields
    private RequestBody requestBody;
    private Priority priority = Priority.NORMAL;
    private RequestFuture<HttpResponse.Builder> futureRequest;
    private Map<String, String> requestHeaders = new HashMap<>();

    // response fields
    private int statusCode;
    private ResponseListener listener;
    private Map<String, String> responseHeaders = new HashMap<>();

    BaseRequest(int method, final String url) {
        super(method, url, null, null);
    }

    void setResponseListener(ResponseListener listener) {
        this.listener = listener;
    }

    void setFutureRequest(RequestFuture<HttpResponse.Builder> futureRequest) {
        this.futureRequest = futureRequest;
    }

    @Override
    public ErrorListener getErrorListener() {
        return listener;
    }

    void addHeader(String key, String value) {
        if (key.equalsIgnoreCase("Content-Type")) {
            PROTOCOL_CONTENT_TYPE = value;
        }
        this.requestHeaders.put(key, value);
    }

    void addHeaders(Map<String, String> map) {
        this.requestHeaders.putAll(map);
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the {@link Priority} of this request; {@link Priority#NORMAL} by default.
     */
    void setPriority(Priority priority) {
        this.priority = priority;
    }

    int getStatusCode() {
        return statusCode;
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.requestHeaders;
    }

    @Override
    protected void deliverResponse(String response) {
        if (futureRequest != null) {
            futureRequest.onResponse(new HttpResponse.Builder()
                    .raw(response).request(this));
            return;
        }
        listener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (futureRequest != null) {
            futureRequest.onErrorResponse(error);
            return;
        }
        this.listener.onErrorResponse(error);
    }

    @Override
    public boolean isCanceled() {
        if (futureRequest != null) {
            return futureRequest.isCancelled();
        }
        return super.isCanceled();
    }

    @Override
    protected com.android.volley.Response<String> parseNetworkResponse(NetworkResponse response) {
        this.statusCode = response.statusCode;
        this.responseHeaders = response.headers;
        /* Get the response data */
        try {
            String json = "";
            if (response.data != null) {
                json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            }
            String log = "%1$s\nResponse code: %2$s\nResponse body: %3$s";
            VolleyLog.v(log, getUrl(), statusCode, json);
            if (statusCode >= 200 && statusCode < 300) {
                /* Return the parsed result in a response wrapper */
                return shouldCache() ?
                        success(json, parseIgnoreCacheHeaders(response)) :
                        success(json, parseCacheHeaders(response));
            } else {
                return error(new ServerError(response));
            }
        } catch (UnsupportedEncodingException e) {
            return error(new ParseError(e));
        }
    }

    void addBody(RequestBody body) {
        this.requestBody = body;
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    okhttp3.RequestBody getRequestBody() {
        return requestBody;
    }

    Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public static class Builder {

        private String url;
        private int method;

        public Builder() {
            this.method = Method.GET;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public BaseRequest build() {
            return new BaseRequest(method, url);
        }
    }
}
