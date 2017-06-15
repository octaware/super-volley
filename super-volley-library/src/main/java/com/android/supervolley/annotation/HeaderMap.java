package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Adds headers specified in the {@link Map}.
 * <p>
 * Values are converted to strings using {@link String#valueOf(Object)}.
 * <p>
 * Simple Example:
 * <pre>
 * &#64;GET("/search")
 * void list(@HeaderMap Map&lt;String, String&gt; headers);
 *
 * ...
 *
 * // The following call yields /search with headers
 * // Accept: text/plain and Accept-Charset: utf-8
 * foo.list(ImmutableMap.of("Accept", "text/plain", "Accept-Charset", "utf-8"));
 * </pre>
 *
 * @see Header
 * @see Headers
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface HeaderMap {

}
