package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named key/value pairs for a form-encoded request.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;FormUrlEncoded
 * &#64;POST("/things")
 * Call&lt;ResponseBody&gt; things(@FieldMap Map&lt;String, String&gt; fields);
 * </code></pre>
 * Calling with {@code foo.things(ImmutableMap.of("foo", "bar", "kit", "kat")} yields a request
 * body of {@code foo=bar&kit=kat}.
 * <p>
 * A {@code null} value for the map, as a key, or as a value is not allowed.
 *
 * @see FormUrlEncoded
 * @see Field
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface FieldMap {
    /**
     * Specifies whether the names and values are already URL encoded.
     */
    boolean encoded() default false;
}
