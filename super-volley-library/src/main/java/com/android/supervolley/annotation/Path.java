package com.android.supervolley.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named replacement in a URL path segment. Values are converted to string using
 * {@link String#valueOf(Object)} and URL encoded.
 * <p>
 * Simple example:
 * <pre><code>
 * &#64;GET("/image/{id}")
 * Call&lt;ResponseBody&gt; example(@Path("id") int id);
 * </code></pre>
 * Calling with {@code foo.example(1)} yields {@code /image/1}.
 * <p>
 * Values are URL encoded by default. Disable with {@code encoded=true}.
 * <pre><code>
 * &#64;GET("/user/{name}")
 * Call&lt;ResponseBody&gt; encoded(@Path("name") String name);
 *
 * &#64;GET("/user/{name}")
 * Call&lt;ResponseBody&gt; notEncoded(@Path(value="name", encoded=true) String name);
 * </code></pre>
 * Calling {@code foo.encoded("John+Doe")} yields {@code /user/John%2BDoe} whereas
 * {@code foo.notEncoded("John+Doe")} yields {@code /user/John+Doe}.
 * <p>
 * Path parameters may not be {@code null}.
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Path {
    String value();

    /**
     * Specifies whether the argument value to the annotated method parameter is already URL encoded.
     */
    boolean encoded() default false;
}
