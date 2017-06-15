package com.android.supervolley.sample.samples.deserialize_error;

import com.android.supervolley.Call;
import com.android.supervolley.Converter;
import com.android.supervolley.Response;
import com.android.supervolley.SuperVolley;
import com.android.supervolley.converter.gson.GsonConverterFactory;
import com.android.supervolley.mock.BehaviorDelegate;
import com.android.supervolley.mock.MockSuperVolley;
import com.android.supervolley.mock.NetworkBehavior;
import com.android.supervolley.sample.constants.Constants;
import com.android.supervolley.sample.samples.deserialize_error.api.MockService;
import com.android.supervolley.sample.samples.deserialize_error.api.Service;
import com.android.supervolley.sample.samples.deserialize_error.model.Error;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;

public final class DeserializeErrorBody {

    public static void main(String... args) throws IOException {
        // Create our Service instance with a SuperVolley pointing at the local web server and Gson.
        SuperVolley volley = new SuperVolley.Builder()
                .baseUrl(Constants.API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create a MockSuperVolley object with a NetworkBehavior which manages the fake behavior of calls.
        NetworkBehavior behavior = NetworkBehavior.create();
        MockSuperVolley mockSuperVolley = new MockSuperVolley.Builder(volley)
                .networkBehavior(behavior)
                .build();

        BehaviorDelegate<Service> delegate = mockSuperVolley.create(Service.class);
        MockService service = new MockService(delegate);

        Call<Void> call = service.getUser();

        Response<Void> response = call.execute();

        // Normally you would check response.isSuccess() here before doing the following, but we know
        // this call will always fail. You could also use response.code() to determine whether to
        // convert the error body and/or which type to use for conversion.

        // Look up a converter for the Error type on the SuperVolley instance.
        Converter<ResponseBody, Error> errorConverter =
                volley.responseBodyConverter(Error.class, new Annotation[0]);
        // Convert the error body into our Error type.
        Error error = errorConverter.convert(response.errorBody());
        System.out.println("ERROR: " + error.message);
    }
}
