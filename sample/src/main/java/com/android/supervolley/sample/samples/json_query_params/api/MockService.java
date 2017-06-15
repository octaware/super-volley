package com.android.supervolley.sample.samples.json_query_params.api;


import com.android.supervolley.Call;
import com.android.supervolley.annotation.Query;
import com.android.supervolley.mock.BehaviorDelegate;
import com.android.supervolley.sample.samples.json_query_params.annotations.Json;
import com.android.supervolley.sample.samples.json_query_params.model.Filter;
import com.android.supervolley.sample.samples.json_query_params.model.User;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;

public class MockService implements Service {

    private final BehaviorDelegate<Service> delegate;
    private final List<User> users;

    public MockService(BehaviorDelegate<Service> delegate) {
        this.delegate = delegate;
        this.users = new ArrayList<>();
        users.add(new User(1, "Test"));
        users.add(new User(2, "Test2"));
        users.add(new User(3, "Test3"));
        users.add(new User(4, "Test4"));
    }

    @Override
    public Call<ResponseBody> example(@Json @Query("value") Filter value) {
        User user = null;
        for (User u : users) {
            if (u.id == value.userId) {
                user = u;
            }
        }
        return delegate.returningResponse(user).example(value);
    }
}
