package com.android.supervolley.sample.samples.json_query_params.api;


import com.android.supervolley.Call;
import com.android.supervolley.annotation.GET;
import com.android.supervolley.annotation.Query;
import com.android.supervolley.sample.samples.json_query_params.annotations.Json;
import com.android.supervolley.sample.samples.json_query_params.model.Filter;

import okhttp3.ResponseBody;

public interface Service {

    @GET("/filter")
    Call<ResponseBody> example(@Json @Query("value") Filter value);
}
