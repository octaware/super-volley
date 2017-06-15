package com.android.supervolley.sample.samples.mock;


import com.android.supervolley.Call;
import com.android.supervolley.SuperVolley;
import com.android.supervolley.mock.BehaviorDelegate;
import com.android.supervolley.mock.MockSuperVolley;
import com.android.supervolley.mock.NetworkBehavior;
import com.android.supervolley.sample.constants.Constants;
import com.android.supervolley.sample.samples.mock.api.GitHub;
import com.android.supervolley.sample.samples.mock.api.MockGitHub;
import com.android.supervolley.sample.samples.mock.model.Contributor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleMockService {

    public static void main(String... args) throws IOException {
        // Create a very simple SuperVolley adapter which points the GitHub API.
        SuperVolley volley = new SuperVolley.Builder()
                .baseUrl(Constants.API_URL)
                .build();

        // Create a MockSuperVolley object with a NetworkBehavior which manages the fake behavior of calls.
        NetworkBehavior behavior = NetworkBehavior.create();
        MockSuperVolley mockSuperVolley = new MockSuperVolley.Builder(volley)
                .networkBehavior(behavior)
                .build();

        BehaviorDelegate<GitHub> delegate = mockSuperVolley.create(GitHub.class);
        MockGitHub gitHub = new MockGitHub(delegate);

        // Query for some contributors for a few repositories.
        printContributors(gitHub, "octa", "tesla");
        printContributors(gitHub, "octa", "picasso");

        // Using the mock-only methods, add some additional data.
        System.out.println("Adding more mock data...\n");
        gitHub.addContributor("octa", "tesla", "Foo Bar", 61);
        gitHub.addContributor("octa", "picasso", "Kit Kat", 53);

        // Reduce the delay to make the next calls complete faster.
        behavior.setDelay(500, TimeUnit.MILLISECONDS);

        // Query for the contributors again so we can see the mock data that was added.
        printContributors(gitHub, "octa", "tesla");
        printContributors(gitHub, "octa", "picasso");
    }

    private static void printContributors(GitHub gitHub, String owner, String repo)
            throws IOException {
        System.out.println(String.format("== Contributors for %s/%s ==", owner, repo));
        Call<List<Contributor>> contributors = gitHub.contributors(owner, repo);
        for (Contributor contributor : contributors.execute().body()) {
            System.out.println(contributor.login + " (" + contributor.contributions + ")");
        }
        System.out.println();
    }

}
