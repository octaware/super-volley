package com.android.supervolley.sample.samples.error_handling;

import com.android.supervolley.Call;
import com.android.supervolley.CallAdapter;
import com.android.supervolley.Response;
import com.android.supervolley.SuperVolley;
import com.android.supervolley.converter.gson.GsonConverterFactory;
import com.android.supervolley.sample.samples.error_handling.adapter.MyCall;
import com.android.supervolley.sample.samples.error_handling.adapter.MyCallback;
import com.android.supervolley.sample.samples.error_handling.api.HttpBinService;
import com.android.supervolley.sample.samples.error_handling.factory.ErrorHandlingCallAdapterFactory;
import com.android.supervolley.sample.samples.error_handling.model.Ip;

import java.io.IOException;

/**
 * A sample showing a custom {@link CallAdapter} which adapts the built-in {@link Call} to a custom
 * version whose callback has more granular methods.
 */
public final class ErrorHandlingSample {

    public static void main(String... args) {
        SuperVolley volley = new SuperVolley.Builder()
                .baseUrl("http://httpbin.org")
                .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpBinService service = volley.create(HttpBinService.class);
        MyCall<Ip> ip = service.getIp();
        ip.enqueue(new MyCallback<Ip>() {
            @Override
            public void success(Response<Ip> response) {
                System.out.println("SUCCESS! " + response.body().origin);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                System.out.println("UNAUTHENTICATED");
            }

            @Override
            public void clientError(Response<?> response) {
                System.out.println("CLIENT ERROR " + response.code() + " " + response.message());
            }

            @Override
            public void serverError(Response<?> response) {
                System.out.println("SERVER ERROR " + response.code() + " " + response.message());
            }

            @Override
            public void networkError(IOException e) {
                System.err.println("NETOWRK ERROR " + e.getMessage());
            }

            @Override
            public void unexpectedError(Throwable t) {
                System.err.println("FATAL ERROR " + t.getMessage());
            }
        });
    }
}
