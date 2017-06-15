package com.android.supervolley;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;

import java.io.IOException;
import java.util.Map;


interface OkHttpStack {

    okhttp3.Response performRequest(Request<?> var1, Map<String, String> var2)
            throws IOException, AuthFailureError;
}