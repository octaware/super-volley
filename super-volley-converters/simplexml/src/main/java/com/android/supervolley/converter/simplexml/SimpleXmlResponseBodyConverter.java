package com.android.supervolley.converter.simplexml;

import com.android.supervolley.Converter;

import org.simpleframework.xml.Serializer;

import java.io.IOException;

import okhttp3.ResponseBody;

final class SimpleXmlResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Class<T> cls;
    private final Serializer serializer;
    private final boolean strict;

    SimpleXmlResponseBodyConverter(Class<T> cls, Serializer serializer, boolean strict) {
        this.cls = cls;
        this.serializer = serializer;
        this.strict = strict;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        try {
            T read = serializer.read(cls, value.charStream(), strict);
            if (read == null) {
                throw new IllegalStateException("Could not deserialize body as " + cls);
            }
            return read;
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            value.close();
        }
    }
}
