package com.android.supervolley.annotation;

import com.android.volley.Request;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Use this annotation on a service method when you want to set its priority in
 * the request queue. By default the priority is set to NORMAL {@link Request.Priority}.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Priority {

    Request.Priority value() default Request.Priority.NORMAL;
}
