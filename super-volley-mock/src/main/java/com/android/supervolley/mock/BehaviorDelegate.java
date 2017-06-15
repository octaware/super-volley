package com.android.supervolley.mock;

import com.android.supervolley.Call;
import com.android.supervolley.CallAdapter;
import com.android.supervolley.SuperVolley;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;

/**
 * Applies {@linkplain NetworkBehavior behavior} to responses and adapts them into the appropriate
 * return type using the {@linkplain SuperVolley#callAdapterFactories() call adapters} of
 * {@link SuperVolley}.
 *
 * @see MockSuperVolley#create(Class)
 */
public final class BehaviorDelegate<T> {
    final SuperVolley volley;
    private final NetworkBehavior behavior;
    private final ExecutorService executor;
    private final Class<T> service;

    BehaviorDelegate(SuperVolley volley, NetworkBehavior behavior, ExecutorService executor,
                     Class<T> service) {
        this.volley = volley;
        this.behavior = behavior;
        this.executor = executor;
        this.service = service;
    }

    public T returningString(String response) {
        return returning(Calls.response(response));
    }

    public T returningResponse(Object response) {
        return returning(Calls.response(response));
    }

    public T returningError(int code, String error) {
        return returning(Calls.failure(code, error));
    }

    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    public <R> T returning(Call<R> call) {
        final Call<R> behaviorCall = new BehaviorCall<>(behavior, executor, call);
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class[]{service},
                new InvocationHandler() {
                    @Override
                    public T invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Type returnType = method.getGenericReturnType();
                        Annotation[] methodAnnotations = method.getAnnotations();
                        CallAdapter<R, T> callAdapter =
                                (CallAdapter<R, T>) volley.callAdapter(returnType, methodAnnotations);
                        return callAdapter.adapt(behaviorCall);
                    }
                });
    }
}
