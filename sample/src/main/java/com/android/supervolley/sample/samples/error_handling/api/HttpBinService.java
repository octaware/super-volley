package com.android.supervolley.sample.samples.error_handling.api;


import com.android.supervolley.annotation.GET;
import com.android.supervolley.sample.samples.error_handling.adapter.MyCall;
import com.android.supervolley.sample.samples.error_handling.model.Ip;

public interface HttpBinService {
    @GET("/ip")
    MyCall<Ip> getIp();
}
