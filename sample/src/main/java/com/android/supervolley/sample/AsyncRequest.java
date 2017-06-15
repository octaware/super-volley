package com.android.supervolley.sample;


import android.os.AsyncTask;

import com.android.supervolley.BaseRequest;
import com.android.supervolley.Call;
import com.android.supervolley.HttpResponse;
import com.android.supervolley.Response;

import java.io.IOException;

public class AsyncRequest<T> extends AsyncTask<Void, Void, Response<T>> {

    private Call<T> call;
    private Callback<T> callback;

    public AsyncRequest(Call<T> call, Callback<T> callback) {
        this.call = call;
        this.callback = callback;
    }

    @Override
    protected Response<T> doInBackground(Void... voids) {
        try {
            return call.execute();
        } catch (IOException e) {
            return Response.error(new HttpResponse.Builder()
                    .message(e.getMessage())
                    .success(false).code(-1)
                    .request(new BaseRequest.Builder().url("URL").build())
                    .build());
        }
    }

    @Override
    protected void onPostExecute(Response<T> object) {
        super.onPostExecute(object);
        callback.onComplete(object);
    }

    public interface Callback<T> {
        void onComplete(Response<T> response);
    }
}
