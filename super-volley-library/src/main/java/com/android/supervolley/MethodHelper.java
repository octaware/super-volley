package com.android.supervolley;

import com.android.volley.Request;

class MethodHelper {

    static int translateMethod(String method) {
        switch (method) {
            case "GET":
                return Request.Method.GET;

            case "DELETE":
                return Request.Method.DELETE;

            case "Multipart":
            case "POST":
                return Request.Method.POST;

            case "PUT":
                return Request.Method.PUT;

            case "HEAD":
                return Request.Method.HEAD;

            case "OPTIONS":
                return Request.Method.OPTIONS;

            case "TRACE":
                return Request.Method.TRACE;

            case "PATCH":
                return Request.Method.PATCH;

            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }
}
