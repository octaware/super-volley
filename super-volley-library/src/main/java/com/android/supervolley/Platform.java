package com.android.supervolley;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

class Platform {
    private static final Platform PLATFORM = findPlatform();

    static Platform get() {
        return PLATFORM;
    }

    private static Platform findPlatform() {
        try {
            Class.forName("android.os.Build");
            if (Build.VERSION.SDK_INT != 0) {
                return new Android();
            }
        } catch (ClassNotFoundException ignored) {
        }
        return new Platform();
    }

    Executor defaultCallbackExecutor() {
        return null;
    }

    CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
        if (callbackExecutor != null) {
            return new ExecutorCallAdapterFactory(callbackExecutor);
        }
        return DefaultCallAdapterFactory.INSTANCE;
    }

    boolean isDefaultMethod(Method method) {
        return false;
    }

    Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object, Object... args)
            throws Throwable {
        throw new UnsupportedOperationException();
    }

    static class Android extends Platform {
        @Override
        public Executor defaultCallbackExecutor() {
            return new MainThreadExecutor();
        }

        @Override
        CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
            return new ExecutorCallAdapterFactory(callbackExecutor);
        }

        static class MainThreadExecutor implements Executor {
            private final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void execute(Runnable r) {
                handler.post(r);
            }
        }
    }
}
