package com.android.supervolley.sample.samples.deserialize_error.api;

import com.android.supervolley.Call;
import com.android.supervolley.annotation.GET;

public interface Service {

    @GET("/user")
    Call<Void> getUser();
}
