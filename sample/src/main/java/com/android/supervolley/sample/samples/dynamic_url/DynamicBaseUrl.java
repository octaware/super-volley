package com.android.supervolley.sample.samples.dynamic_url;

import com.android.supervolley.Response;
import com.android.supervolley.SuperVolley;
import com.android.supervolley.sample.AsyncRequest;
import com.android.supervolley.sample.samples.dynamic_url.api.HostSelectionInterceptor;
import com.android.supervolley.sample.samples.dynamic_url.api.Pop;

import java.io.IOException;

import okhttp3.ResponseBody;


/**
 * This example uses an OkHttp interceptor to change the target hostname dynamically at runtime.
 * Typically this would be used to implement client-side load balancing or to use the webserver
 * that's nearest geographically.
 */
public final class DynamicBaseUrl {

    public static void main(String... args) throws IOException {
        final HostSelectionInterceptor hostSelectionInterceptor = new HostSelectionInterceptor();

        SuperVolley volley = new SuperVolley.Builder()
                .baseUrl("http://www.coca-cola.com/")
                .interceptor(hostSelectionInterceptor)
                .build();

        final Pop pop = volley.create(Pop.class);

        new AsyncRequest<>(pop.robots(), new AsyncRequest.Callback<ResponseBody>() {
            @Override
            public void onComplete(Response<ResponseBody> response) {
                System.out.println("Request for: " + response.raw().request());
                try {
                    if (response.body() != null)
                        System.out.println(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).execute();

        hostSelectionInterceptor.setHost("www.pepsi.com");

        new AsyncRequest<>(pop.robots(), new AsyncRequest.Callback<ResponseBody>() {
            @Override
            public void onComplete(Response<ResponseBody> response) {
                System.out.println("Request for: " + response.raw().request());
                try {
                    if (response.body() != null)
                        System.out.println(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).execute();
    }
}
