package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Adds headers literally supplied in the {@code value}.
 * <pre><code>
 * &#64;Headers("Cache-Control: max-age=640000")
 * &#64;GET("/")
 * ...
 *
 * &#64;Headers({
 *   "X-Foo: Bar",
 *   "X-Ping: Pong"
 * })
 * &#64;GET("/")
 * ...
 * </code></pre>
 * <strong>Note:</strong> Headers do not overwrite each other. All headers with the same name will
 * be included in the request.
 *
 * @see Header
 * @see HeaderMap
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Headers {
    String[] value();
}
