package com.android.supervolley.converter.simplexml;

import com.android.supervolley.Converter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * A {@linkplain Converter.Factory converter} which uses Simple Framework for XML.
 * <p>
 * This converter only applies for class types. Parameterized types (e.g., {@code List<Foo>}) are
 * not handled.
 */
public final class SimpleXmlConverterFactory extends Converter.Factory {
    /**
     * Create an instance using a default {@link Persister} instance for conversion.
     */
    public static SimpleXmlConverterFactory create() {
        return create(new Persister());
    }

    /**
     * Create an instance using {@code serializer} for conversion.
     */
    public static SimpleXmlConverterFactory create(Serializer serializer) {
        return new SimpleXmlConverterFactory(serializer, true);
    }

    /**
     * Create an instance using a default {@link Persister} instance for non-strict conversion.
     */
    public static SimpleXmlConverterFactory createNonStrict() {
        return createNonStrict(new Persister());
    }

    /**
     * Create an instance using {@code serializer} for non-strict conversion.
     */
    public static SimpleXmlConverterFactory createNonStrict(Serializer serializer) {
        return new SimpleXmlConverterFactory(serializer, false);
    }

    private final Serializer serializer;
    private final boolean strict;

    private SimpleXmlConverterFactory(Serializer serializer, boolean strict) {
        if (serializer == null) throw new NullPointerException("serializer == null");
        this.serializer = serializer;
        this.strict = strict;
    }

    public boolean isStrict() {
        return strict;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations) {
        if (!(type instanceof Class)) {
            return null;
        }
        Class<?> cls = (Class<?>) type;
        return new SimpleXmlResponseBodyConverter<>(cls, serializer, strict);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations) {
        if (!(type instanceof Class)) {
            return null;
        }
        return new SimpleXmlRequestBodyConverter<>(serializer);
    }
}
