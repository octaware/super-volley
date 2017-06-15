package com.android.supervolley;

import com.android.supervolley.BaseRequest;

import okhttp3.Headers;
import okhttp3.ResponseBody;

/**
 * An HTTP response.
 */
public final class Response<T> {
    /**
     * Create a synthetic successful response with {@code body} as the deserialized body.
     */
    public static <T> Response<T> success(T body, BaseRequest request) {
        return success(body, new HttpResponse.Builder() //
                .code(200).success(true)
                .message("OK")
                .request(request)
                .build());
    }

    /**
     * Create a synthetic successful response using {@code headers} with {@code body} as the
     * deserialized body.
     */
    public static <T> Response<T> success(T body, Headers headers) {
        if (headers == null) throw new NullPointerException("headers == null");
        return success(body, new HttpResponse.Builder() //
                .code(200).success(true)
                .message("OK")
                .request(new BaseRequest.Builder().url("http://localhost/").build())
                .build());
    }

    /**
     * Create a successful response from {@code rawResponse} with {@code body} as the deserialized
     * body.
     */
    public static <T> Response<T> success(T body, HttpResponse rawResponse) {
        if (rawResponse == null) throw new NullPointerException("rawResponse == null");
        return new Response<>(rawResponse, body, null);
    }

    /**
     * Create a synthetic error response with an HTTP status code of {@code code} and {@code body}
     * as the error body.
     */
    public static <T> Response<T> error(int code, ResponseBody body) {
        if (code < 400) throw new IllegalArgumentException("code < 400: " + code);
        return error(body, new HttpResponse.Builder() //
                .code(code).success(false)
                .request(new BaseRequest.Builder().url("http://localhost/").build())
                .build());
    }

    /**
     * Create an error response from {@code rawResponse} with {@code body} as the error body.
     */
    public static <T> Response<T> error(ResponseBody body, HttpResponse rawResponse) {
        if (body == null) throw new NullPointerException("body == null");
        if (rawResponse == null) throw new NullPointerException("rawResponse == null");
        return new Response<>(rawResponse, null, body);
    }

    public static <T> Response<T> error(HttpResponse rawResponse) {
        if (rawResponse == null) throw new NullPointerException("rawResponse == null");
        return new Response<>(rawResponse, null, null);
    }

    private final HttpResponse rawResponse;
    private final T body;
    private final ResponseBody errorBody;

    private Response(HttpResponse rawResponse, T body, ResponseBody errorBody) {
        this.rawResponse = rawResponse;
        this.body = body;
        this.errorBody = errorBody;
    }

    /**
     * The raw response from the HTTP client.
     */
    public HttpResponse raw() {
        return rawResponse;
    }

    /**
     * HTTP status code.
     */
    public int code() {
        return rawResponse.code();
    }

    /**
     * HTTP status message or null if unknown.
     */
    public String message() {
        return rawResponse.message();
    }

    /**
     * Returns true if {@link #code()} is in the range [200..300).
     */
    public boolean isSuccessful() {
        return rawResponse.isSuccessful();
    }

    /**
     * The deserialized response body of a {@linkplain #isSuccessful() successful} response.
     */
    public T body() {
        return body;
    }

    /**
     * The raw response body of an {@linkplain #isSuccessful() unsuccessful} response.
     */
    public ResponseBody errorBody() {
        return errorBody;
    }

    @Override
    public String toString() {
        return rawResponse.toString();
    }

}
