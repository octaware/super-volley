package com.android.supervolley.sample.samples.rxjava.api;


import com.android.supervolley.annotation.GET;

import io.reactivex.Observable;
import okhttp3.ResponseBody;

public interface RxService {

    @GET("robots.txt")
    Observable<ResponseBody> robots();
}
