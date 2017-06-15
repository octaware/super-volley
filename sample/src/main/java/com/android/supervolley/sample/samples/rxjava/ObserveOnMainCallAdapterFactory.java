package com.android.supervolley.sample.samples.rxjava;


import com.android.supervolley.Call;
import com.android.supervolley.CallAdapter;
import com.android.supervolley.SuperVolley;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

final class ObserveOnMainCallAdapterFactory extends CallAdapter.Factory {
    private final Scheduler scheduler;

    ObserveOnMainCallAdapterFactory(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, SuperVolley volley) {
        if (getRawType(returnType) != Observable.class) {
            return null; // Ignore non-Observable types.
        }

        // Look up the next call adapter which would otherwise be used if this one was not present.
        //noinspection unchecked returnType checked above to be Observable.
        final CallAdapter<Object, Observable<?>> delegate =
                (CallAdapter<Object, Observable<?>>) volley.nextCallAdapter(this, returnType,
                        annotations);

        return new CallAdapter<Object, Object>() {
            @Override
            public Object adapt(Call<Object> call) {
                // Delegate to get the normal Observable...
                Observable<?> o = delegate.adapt(call);
                // ...and change it to send notifications to the observer on the specified scheduler.
                return o.observeOn(scheduler);
            }

            @Override
            public Type responseType() {
                return delegate.responseType();
            }
        };
    }
}
