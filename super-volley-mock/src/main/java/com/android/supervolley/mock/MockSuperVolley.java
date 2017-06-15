package com.android.supervolley.mock;

import com.android.supervolley.SuperVolley;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MockSuperVolley {
    private final SuperVolley volley;
    private final NetworkBehavior behavior;
    private final ExecutorService executor;

    MockSuperVolley(SuperVolley volley, NetworkBehavior behavior, ExecutorService executor) {
        this.volley = volley;
        this.behavior = behavior;
        this.executor = executor;
    }

    public SuperVolley superVolley() {
        return volley;
    }

    public NetworkBehavior networkBehavior() {
        return behavior;
    }

    public Executor backgroundExecutor() {
        return executor;
    }

    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    public <T> BehaviorDelegate<T> create(Class<T> service) {
        return new BehaviorDelegate<>(volley, behavior, executor, service);
    }

    public static final class Builder {
        private final SuperVolley volley;
        private NetworkBehavior behavior;
        private ExecutorService executor;

        public Builder(SuperVolley volley) {
            if (volley == null) throw new NullPointerException("volley == null");
            this.volley = volley;
        }

        public Builder networkBehavior(NetworkBehavior behavior) {
            if (behavior == null) throw new NullPointerException("behavior == null");
            this.behavior = behavior;
            return this;
        }

        public Builder backgroundExecutor(ExecutorService executor) {
            if (executor == null) throw new NullPointerException("executor == null");
            this.executor = executor;
            return this;
        }

        public MockSuperVolley build() {
            if (behavior == null) behavior = NetworkBehavior.create();
            if (executor == null) executor = Executors.newCachedThreadPool();
            return new MockSuperVolley(volley, behavior, executor);
        }
    }
}
