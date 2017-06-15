package com.android.supervolley.sample.samples.json_and_xml.api;


import com.android.supervolley.Call;
import com.android.supervolley.annotation.GET;
import com.android.supervolley.sample.samples.json_and_xml.annotations.Json;
import com.android.supervolley.sample.samples.json_and_xml.annotations.Xml;

public interface Service {

    @GET("/")
    @Json
    Call<String> exampleJson();

    @GET("/")
    @Xml
    Call<String> exampleXml();
}
