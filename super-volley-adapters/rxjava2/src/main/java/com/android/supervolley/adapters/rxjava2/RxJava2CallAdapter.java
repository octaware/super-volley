package com.android.supervolley.adapters.rxjava2;

import com.android.supervolley.Call;
import com.android.supervolley.CallAdapter;
import com.android.supervolley.Response;

import java.lang.reflect.Type;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

final class RxJava2CallAdapter<R> implements CallAdapter<R, Object> {
    private final Type responseType;
    private final Scheduler scheduler;
    private final boolean isResult;
    private final boolean isBody;
    private final boolean isFlowable;
    private final boolean isSingle;
    private final boolean isMaybe;
    private final boolean isCompletable;

    RxJava2CallAdapter(Type responseType, Scheduler scheduler, boolean isResult, boolean isBody,
                       boolean isFlowable, boolean isSingle, boolean isMaybe, boolean isCompletable) {
        this.responseType = responseType;
        this.scheduler = scheduler;
        this.isResult = isResult;
        this.isBody = isBody;
        this.isFlowable = isFlowable;
        this.isSingle = isSingle;
        this.isMaybe = isMaybe;
        this.isCompletable = isCompletable;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public Object adapt(Call<R> call) {
        Observable<Response<R>> responseObservable = new CallObservable<>(call);

        Observable<?> observable;
        if (isResult) {
            observable = new ResultObservable<>(responseObservable);
        } else if (isBody) {
            observable = new BodyObservable<>(responseObservable);
        } else {
            observable = responseObservable;
        }

        if (scheduler != null) {
            observable = observable.subscribeOn(scheduler);
        }

        if (isFlowable) {
            return observable.toFlowable(BackpressureStrategy.LATEST);
        }
        if (isSingle) {
            return observable.singleOrError();
        }
        if (isMaybe) {
            return observable.singleElement();
        }
        if (isCompletable) {
            return observable.ignoreElements();
        }
        return observable;
    }
}
