package com.android.supervolley.annotation;

import com.android.supervolley.Converter;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import okhttp3.RequestBody;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes a single part of a multi-part request.
 * <p>
 * The parameter type on which this annotation exists will be processed in one of three ways:
 * <ul>
 * <li>If the type is {@link okhttp3.MultipartBody.Part} the contents will be used directly. Omit
 * the name from the annotation (i.e., {@code @Part MultipartBody.Part part}).</li>
 * <li>If the type is {@link RequestBody RequestBody} the value will be used
 * directly with its content type. Supply the part name in the annotation (e.g.,
 * {@code @Part("foo") RequestBody foo}).</li>
 * <li>Other object types will be converted to an appropriate representation by using
 * {@linkplain Converter a converter}. Supply the part name in the annotation (e.g.,
 * {@code @Part("foo") Image photo}).</li>
 * </ul>
 * <p>
 * Values may be {@code null} which will omit them from the request body.
 * <p>
 * <pre><code>
 * &#64;Multipart
 * &#64;POST("/")
 * Call&lt;ResponseBody&gt; example(
 *     &#64;Part("description") String description,
 *     &#64;Part(value = "image", encoding = "8-bit") RequestBody image);
 * </code></pre>
 * <p>
 * Part parameters may not be {@code null}.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Part {
    /**
     * The name of the part. Required for all parameter types except
     * {@link okhttp3.MultipartBody.Part}.
     */
    String value() default "";

    /**
     * The {@code Content-Transfer-Encoding} of this part.
     */
    String encoding() default "binary";
}
