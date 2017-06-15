package com.android.supervolley;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Creates call adapters for that uses the same thread for both I/O and application-level
 * callbacks. For synchronous calls this is the application thread making the request; for
 * asynchronous calls this is a thread provided by OkHttp's dispatcher.
 */
final class DefaultCallAdapterFactory extends CallAdapter.Factory {
    static final CallAdapter.Factory INSTANCE = new DefaultCallAdapterFactory();

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, SuperVolley volley) {
        if (getRawType(returnType) != Call.class) {
            return null;
        }

        final Type responseType = Utils.getCallResponseType(returnType);
        return new CallAdapter<Object, Call<?>>() {
            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public Call<Object> adapt(Call<Object> call) {
                return call;
            }
        };
    }
}
