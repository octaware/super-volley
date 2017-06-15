package com.android.supervolley.sample;


import android.app.Activity;
import android.os.Bundle;

import com.android.supervolley.sample.samples.deserialize_error.DeserializeErrorBody;
import com.android.supervolley.sample.samples.dynamic_url.DynamicBaseUrl;
import com.android.supervolley.sample.samples.json_and_xml.JsonAndXmlConverters;
import com.android.supervolley.sample.samples.mock.SimpleMockService;
import com.android.supervolley.sample.samples.rxjava.RxJavaObserveOnMainThread;

import java.io.IOException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxJavaObserveOnMainThread.main();
        try {
            SimpleMockService.main();
            DeserializeErrorBody.main();
            DynamicBaseUrl.main();
            JsonAndXmlConverters.main();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
