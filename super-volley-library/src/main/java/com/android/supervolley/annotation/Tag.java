package com.android.supervolley.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Set a tag on this request. Can be used to cancel all requests with this tag.
 * By default the tag is the relative url of the request.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Tag {

    String value();
}
