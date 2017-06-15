package com.android.supervolley.sample.samples.dynamic_url.api;


import com.android.supervolley.Call;
import com.android.supervolley.annotation.GET;

import okhttp3.ResponseBody;

public interface Pop {
    @GET("robots.txt")
    Call<ResponseBody> robots();
}
