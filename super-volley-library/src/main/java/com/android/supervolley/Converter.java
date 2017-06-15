package com.android.supervolley;

import com.android.supervolley.annotation.Body;
import com.android.supervolley.annotation.Field;
import com.android.supervolley.annotation.FieldMap;
import com.android.supervolley.annotation.Header;
import com.android.supervolley.annotation.HeaderMap;
import com.android.supervolley.annotation.Part;
import com.android.supervolley.annotation.PartMap;
import com.android.supervolley.annotation.Path;
import com.android.supervolley.annotation.Query;
import com.android.supervolley.annotation.QueryMap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Convert objects to and from their representation in HTTP. Instances are created by {@linkplain
 * Factory a factory} which is {@linkplain SuperVolley.Builder#addConverterFactory(Factory) installed}
 * into the {@link SuperVolley} instance.
 */
public interface Converter<F, T> {
    T convert(F value) throws IOException;

    /**
     * Creates {@link Converter} instances based on a type and target usage.
     */
    abstract class Factory {
        /**
         * Returns a {@link Converter} for converting an HTTP response body to {@code type}, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for
         * response types such as {@code SimpleResponse} from a {@code Call<SimpleResponse>}
         * declaration.
         */
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations) {
            return null;
        }

        /**
         * Returns a {@link Converter} for converting {@code type} to an HTTP request body, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for types
         * specified by {@link Body @Body}, {@link Part @Part}, and {@link PartMap @PartMap}
         * values.
         */
        public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations) {
            return null;
        }

        /**
         * Returns a {@link Converter} for converting {@code type} to a {@link String}, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for types
         * specified by {@link Field @Field}, {@link FieldMap @FieldMap} values,
         * {@link Header @Header}, {@link HeaderMap @HeaderMap}, {@link Path @Path},
         * {@link Query @Query}, and {@link QueryMap @QueryMap} values.
         */
        public Converter<?, String> stringConverter(Type type, Annotation[] annotations) {
            return null;
        }
    }
}
