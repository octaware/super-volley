package com.android.supervolley.sample.samples.json_query_params;

import com.android.supervolley.Call;
import com.android.supervolley.Response;
import com.android.supervolley.SuperVolley;
import com.android.supervolley.converter.gson.GsonConverterFactory;
import com.android.supervolley.mock.BehaviorDelegate;
import com.android.supervolley.mock.MockSuperVolley;
import com.android.supervolley.mock.NetworkBehavior;
import com.android.supervolley.sample.constants.Constants;
import com.android.supervolley.sample.samples.json_query_params.api.MockService;
import com.android.supervolley.sample.samples.json_query_params.api.Service;
import com.android.supervolley.sample.samples.json_query_params.model.Filter;

import java.io.IOException;

import okhttp3.ResponseBody;


public final class JsonQueryParameters {

    public static void main(String... args) throws IOException, InterruptedException {
        SuperVolley volley = new SuperVolley.Builder()
                .baseUrl(Constants.API_URL)
                .addConverterFactory(new JsonStringConverterFactory(GsonConverterFactory.create()))
                .build();

        // Create a MockSuperVolley object with a NetworkBehavior which manages the fake behavior of calls.
        NetworkBehavior behavior = NetworkBehavior.create();
        MockSuperVolley mockSuperVolley = new MockSuperVolley.Builder(volley)
                .networkBehavior(behavior)
                .build();

        BehaviorDelegate<Service> delegate = mockSuperVolley.create(Service.class);
        MockService service = new MockService(delegate);

        Call<ResponseBody> call = service.example(new Filter(2));

        Response<ResponseBody> response = call.execute();
        // TODO handle user response...

        System.out.println(response.body());
    }
}
