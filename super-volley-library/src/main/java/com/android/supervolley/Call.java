package com.android.supervolley;

import com.android.volley.Request;

import java.io.IOException;

/**
 * An invocation of a SuperVolley method that sends a request to a webserver and returns a response.
 * Each call yields its own HTTP request and response pair. Use {@link #clone} to make multiple
 * calls with the same parameters to the same webserver; this may be used to implement polling or
 * to retry a failed call.
 * <p>
 * <p>Calls may be executed asynchronously with {@link #enqueue}. The call can be canceled at any time
 * with {@link #cancel}. A call that is busy writing its request or reading its response may receive
 * a {@link IOException}; this is working as designed.
 *
 * @param <T> Successful response body type.
 */
public interface Call<T> extends Cloneable {

    /**
     * Synchronously send the request and return its response.
     *
     * @throws IOException      if a problem occurred talking to the server.
     * @throws RuntimeException (and subclasses) if an unexpected error occurs creating the request
     *                          or decoding the response.
     */
    Response<T> execute() throws IOException;

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     */
    void enqueue(Callback<T> callback);

    /**
     * Returns true if this call has been {@linkplain #enqueue(Callback) enqueued}.
     * It is an error to execute or enqueue a call more than once.
     */
    boolean isExecuted();

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    void cancel();

    /**
     * True if {@link #cancel()} was called.
     */
    boolean isCanceled();

    /**
     * Create a new, identical call to this one which can be enqueued or executed even if this call
     * has already been.
     */
    Call<T> clone();

    /**
     * The original HTTP request.
     */
    Request request();

}
