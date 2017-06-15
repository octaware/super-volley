package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Treat the response body on methods returning {@link okhttp3.Response Response} as is,
 * i.e. without converting {@link okhttp3.Response#body() body()} to {@code byte[]}.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Streaming {
}
