package com.android.supervolley;

import com.android.volley.DefaultRetryPolicy;

import java.util.concurrent.TimeUnit;


class NoRetryPolicy extends DefaultRetryPolicy {

    NoRetryPolicy() {
        super((int) TimeUnit.SECONDS.toMillis(60), 0, DEFAULT_BACKOFF_MULT);
    }
}
