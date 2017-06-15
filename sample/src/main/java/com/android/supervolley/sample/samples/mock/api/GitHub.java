package com.android.supervolley.sample.samples.mock.api;


import com.android.supervolley.Call;
import com.android.supervolley.annotation.GET;
import com.android.supervolley.annotation.Path;
import com.android.supervolley.sample.samples.mock.model.Contributor;

import java.util.List;

public interface GitHub {

    @GET("/repos/{owner}/{repo}/contributors")
    Call<List<Contributor>> contributors(
            @Path("owner") String owner,
            @Path("repo") String repo);
}
