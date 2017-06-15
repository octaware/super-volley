package com.android.supervolley.sample.samples.json_and_xml.api;


import com.android.supervolley.Call;
import com.android.supervolley.mock.BehaviorDelegate;

public class MockService implements Service {

    private final BehaviorDelegate<Service> delegate;

    public MockService(BehaviorDelegate<Service> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Call<String> exampleJson() {
        return delegate.returningString("{\"name\":\"Bob\"}").exampleJson();
    }

    @Override
    public Call<String> exampleXml() {
        return delegate.returningString("<user><name>Marley</name></user>").exampleXml();
    }
}
