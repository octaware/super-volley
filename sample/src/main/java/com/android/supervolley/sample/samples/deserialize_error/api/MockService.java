package com.android.supervolley.sample.samples.deserialize_error.api;


import com.android.supervolley.Call;
import com.android.supervolley.mock.BehaviorDelegate;

public class MockService implements Service {

    private final BehaviorDelegate<Service> delegate;

    public MockService(BehaviorDelegate<Service> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Call<Void> getUser() {
        return delegate.returningError(404, "{\"message\":\"Unable to locate resource.\"}").getUser();
    }
}
