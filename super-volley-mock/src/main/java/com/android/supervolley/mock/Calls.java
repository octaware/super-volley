package com.android.supervolley.mock;

import com.android.supervolley.BaseRequest;
import com.android.supervolley.Call;
import com.android.supervolley.Callback;
import com.android.supervolley.Response;
import com.android.volley.Request;

import java.io.IOException;

import okhttp3.ResponseBody;

/**
 * Factory methods for creating {@link Call} instances which immediately respond or fail.
 */
public final class Calls {

    public static <T> Call<T> response(T successValue) {
        return response(Response.success(successValue, new BaseRequest.Builder().url("http://localhost/").build()));
    }

    public static <T> Call<T> response(final Response<T> response) {
        return new Call<T>() {
            @Override
            public Response<T> execute() throws IOException {
                return response;
            }

            @Override
            public void enqueue(Callback<T> callback) {
                callback.onResponse(this, response);
            }

            @Override
            public boolean isExecuted() {
                return false;
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @SuppressWarnings("CloneDoesntCallSuperClone") // Immutable object.
            @Override
            public Call<T> clone() {
                return this;
            }

            @Override
            public Request request() {
                return response.raw().request();
            }
        };
    }

    public static <T> Call<T> failure(int code, String error) {
        return failure(Response.error(code, ResponseBody.create(null, error.getBytes())));
    }

    public static <T> Call<T> failure(final Response response) {
        return new Call<T>() {
            @Override
            public Response<T> execute() throws IOException {
                return response;
            }

            @Override
            public void enqueue(Callback<T> callback) {
                callback.onFailure(this, new IOException("Failure."));
            }

            @Override
            public boolean isExecuted() {
                return false;
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @SuppressWarnings("CloneDoesntCallSuperClone") // Immutable object.
            @Override
            public Call<T> clone() {
                return this;
            }

            @Override
            public Request request() {
                return new BaseRequest.Builder().url("http://localhost").build();
            }
        };
    }

    private Calls() {
        throw new AssertionError("No instances.");
    }
}
