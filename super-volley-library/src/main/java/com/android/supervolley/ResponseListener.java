package com.android.supervolley;

import com.android.volley.Response;
import com.android.volley.VolleyError;

abstract class ResponseListener implements
        Response.Listener<String>, Response.ErrorListener {

    @Override
    public void onResponse(String response) {
        onSuccess(new HttpResponse.Builder().raw(response));
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        onFailure(error);
    }

    protected abstract void onSuccess(HttpResponse.Builder builder);

    abstract void onFailure(VolleyError payload);

}
