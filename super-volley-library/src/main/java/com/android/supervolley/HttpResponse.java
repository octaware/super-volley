package com.android.supervolley;


import com.android.supervolley.BaseRequest;
import com.android.volley.Request;

import java.util.Collections;
import java.util.Map;

public class HttpResponse {

    private int code;
    private String raw;
    private String message;
    private boolean isSuccessful;
    private BaseRequest request;

    private HttpResponse(BaseRequest request, int code, String raw, String message, boolean isSuccessful) {
        this.request = request;
        this.code = code;
        this.raw = raw;
        this.message = message;
        this.isSuccessful = isSuccessful;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public Request request() {
        return request;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> headers() {
        return request != null ? request.getResponseHeaders() : Collections.<String, String>emptyMap();
    }

    @Override
    public String toString() {
        return raw;
    }

    public static class Builder {
        private int code;
        private String raw;
        private String message;
        private boolean isSuccessful;
        private BaseRequest request;

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder request(BaseRequest request) {
            this.request = request;
            return this;
        }

        public Builder success(boolean isSuccessful) {
            this.isSuccessful = isSuccessful;
            return this;
        }

        public Builder raw(String raw) {
            this.raw = raw;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(request, code, raw, message, isSuccessful);
        }
    }
}
