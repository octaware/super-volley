package com.android.supervolley.sample.samples.mock.api;


import com.android.supervolley.Call;
import com.android.supervolley.mock.BehaviorDelegate;
import com.android.supervolley.sample.samples.mock.model.Contributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A mock implementation of the {@link GitHub} API interface.
 */
public final class MockGitHub implements GitHub {
    private final BehaviorDelegate<GitHub> delegate;
    private final Map<String, Map<String, List<Contributor>>> ownerRepoContributors;

    public MockGitHub(BehaviorDelegate<GitHub> delegate) {
        this.delegate = delegate;
        ownerRepoContributors = new LinkedHashMap<>();

        // Seed some mock data.
        addContributor("octa", "tesla", "John Doe", 12);
        addContributor("octa", "tesla", "Bob Smith", 2);
        addContributor("octa", "tesla", "Big Bird", 40);
        addContributor("octa", "picasso", "Proposition Joe", 39);
        addContributor("octa", "picasso", "Keiser Soze", 152);
    }

    @Override
    public Call<List<Contributor>> contributors(String owner, String repo) {
        List<Contributor> response = Collections.emptyList();
        Map<String, List<Contributor>> repoContributors = ownerRepoContributors.get(owner);
        if (repoContributors != null) {
            List<Contributor> contributors = repoContributors.get(repo);
            if (contributors != null) {
                response = contributors;
            }
        }
        return delegate.returningResponse(response).contributors(owner, repo);
    }

    public void addContributor(String owner, String repo, String name, int contributions) {
        Map<String, List<Contributor>> repoContributors = ownerRepoContributors.get(owner);
        if (repoContributors == null) {
            repoContributors = new LinkedHashMap<>();
            ownerRepoContributors.put(owner, repoContributors);
        }
        List<Contributor> contributors = repoContributors.get(repo);
        if (contributors == null) {
            contributors = new ArrayList<>();
            repoContributors.put(repo, contributors);
        }
        contributors.add(new Contributor(name, contributions));
    }
}
