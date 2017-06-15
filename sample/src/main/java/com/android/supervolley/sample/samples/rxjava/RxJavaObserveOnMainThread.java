package com.android.supervolley.sample.samples.rxjava;


import com.android.supervolley.SuperVolley;
import com.android.supervolley.adapters.rxjava2.RxJava2CallAdapterFactory;
import com.android.supervolley.sample.samples.rxjava.api.RxService;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;


public class RxJavaObserveOnMainThread {

    public static void main(String... args) {
        Scheduler observeOn = AndroidSchedulers.mainThread(); // Or use mainThread() for Android.

        SuperVolley volley = new SuperVolley.Builder()
                .baseUrl("http://www.coca-cola.com/")
                .addCallAdapterFactory(new ObserveOnMainCallAdapterFactory(observeOn))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();

        final RxService service = volley.create(RxService.class);

        Observable<ResponseBody> observable = service.robots()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new Observer<ResponseBody>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(ResponseBody value) {
                try {
                    System.out.println(value.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {
                //handle error
            }

            @Override
            public void onComplete() {
                System.out.println("-- request completed --");
            }
        });
    }
}
