package com.android.supervolley.annotation;

import com.android.supervolley.SuperVolley.Builder;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Use this annotation on a service method when you want to cache the response in
 * your default caching mechanism defined in {@link Builder#cache }.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface CacheResponse {
}
