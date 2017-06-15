package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Replaces the header with the value of its target.
 * <pre><code>
 * &#64;GET("/")
 * Call&lt;ResponseBody&gt; foo(@Header("Accept-Language") String lang);
 * </code></pre>
 * Header parameters may be {@code null} which will omit them from the request. Passing a
 * {@link java.util.List List} or array will result in a header for each non-{@code null} item.
 * <p>
 * <strong>Note:</strong> Headers do not overwrite each other. All headers with the same name will
 * be included in the request.
 *
 * @see Headers
 * @see HeaderMap
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Header {
    String value();
}
