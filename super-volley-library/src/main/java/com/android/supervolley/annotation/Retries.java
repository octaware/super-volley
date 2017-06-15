package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Retry policy for requests.
 * <p>
 * e.g:
 * - @Retries(2) -> Retry 2 times
 * - @Retries({1, 1000}) -> Retry 1 time with 1sec delay between
 * - @Retries({3, 500, 2}) -> Retry 3 times with a BACKOFF_MULTIPLIER 2.
 * Which means that the first retry will be after 0.5sec, the second
 * after 1sec and the 3rd after 2seconds.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Retries {

    int[] value() default 0;
}
