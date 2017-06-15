package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Query parameter keys and values appended to the URL.
 * <p>
 * Both keys and values are converted to strings using {@link String#valueOf(Object)}.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;GET("/search")
 * Call&lt;ResponseBody&gt; list(@QueryMap Map&lt;String, String&gt; filters);
 * </code></pre>
 * Calling with {@code foo.list(ImmutableMap.of("foo", "bar", "kit", "kat"))} yields
 * {@code /search?foo=bar&kit=kat}.
 * <p>
 * Map keys and values representing parameter values are URL encoded by default. Specify
 * {@link #encoded() encoded=true} to change this behavior.
 * <pre><code>
 * &#64;GET("/search")
 * Call&lt;ResponseBody&gt; list(@QueryMap(encoded=true) Map&lt;String, String&gt; filters);
 * </code></pre>
 * Calling with {@code foo.list(ImmutableMap.of("foo", "foo+bar"))} yields
 * {@code /search?foo=foo+bar}.
 * <p>
 * A {@code null} value for the map, as a key, or as a value is not allowed.
 *
 * @see Query
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface QueryMap {
    /**
     * Specifies whether parameter names and values are already URL encoded.
     */
    boolean encoded() default false;
}
