package com.android.supervolley.sample.samples.json_and_xml;

import com.android.supervolley.Converter;
import com.android.supervolley.Response;
import com.android.supervolley.SuperVolley;
import com.android.supervolley.converter.gson.GsonConverterFactory;
import com.android.supervolley.converter.simplexml.SimpleXmlConverterFactory;
import com.android.supervolley.mock.BehaviorDelegate;
import com.android.supervolley.mock.MockSuperVolley;
import com.android.supervolley.mock.NetworkBehavior;
import com.android.supervolley.sample.AsyncRequest;
import com.android.supervolley.sample.constants.Constants;
import com.android.supervolley.sample.samples.json_and_xml.api.MockService;
import com.android.supervolley.sample.samples.json_and_xml.api.Service;
import com.android.supervolley.sample.samples.json_and_xml.model.User;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;

/**
 * Both the Gson converter and the Simple Framework converter accept all types. Because of this,
 * you cannot use both in a single service by default. In order to work around this, we can create
 * an @Json and @Xml annotation to declare which serialization format each endpoint should use and
 * then write our own Converter.Factory which delegates to either the Gson or Simple Framework
 * converter.
 */
public final class JsonAndXmlConverters {

    public static void main(String... args) throws IOException {
        final SuperVolley volley = new SuperVolley.Builder()
                .baseUrl(Constants.API_URL)
                .addConverterFactory(new QualifiedTypeConverterFactory(
                        GsonConverterFactory.create(),
                        SimpleXmlConverterFactory.create()))
                .build();

        // Create a MockSuperVolley object with a NetworkBehavior which manages the fake behavior of calls.
        NetworkBehavior behavior = NetworkBehavior.create();
        MockSuperVolley mockSuperVolley = new MockSuperVolley.Builder(volley)
                .networkBehavior(behavior)
                .build();

        BehaviorDelegate<Service> delegate = mockSuperVolley.create(Service.class);
        MockService service = new MockService(delegate);

        new AsyncRequest<>(service.exampleJson(), new AsyncRequest.Callback<String>() {
            @Override
            public void onComplete(Response<String> response) {
                try {
                    Annotation[] annotations = Service.class.getMethod("exampleJson").getAnnotations();
                    Converter<ResponseBody, User> userConverter = volley.responseBodyConverter(User.class, annotations);
                    User user = userConverter.convert(ResponseBody.create(null, response.body()));
                    System.out.println("User 1: " + user.name);
                } catch (NoSuchMethodException | IOException ignored) {
                }
            }
        }).execute();

        new AsyncRequest<>(service.exampleXml(), new AsyncRequest.Callback<String>() {
            @Override
            public void onComplete(Response<String> response) {
                try {
                    Annotation[] annotations = Service.class.getMethod("exampleXml").getAnnotations();
                    Converter<ResponseBody, User> userConverter = volley.responseBodyConverter(User.class, annotations);
                    User user = userConverter.convert(ResponseBody.create(null, response.body()));
                    System.out.println("User 2: " + user.name);
                } catch (NoSuchMethodException | IOException ignored) {
                }
            }
        }).execute();
    }
}
