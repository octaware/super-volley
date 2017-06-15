package com.android.supervolley.converter.jackson;

import com.android.supervolley.Converter;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

import okhttp3.ResponseBody;

final class JacksonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final ObjectReader adapter;

    JacksonResponseBodyConverter(ObjectReader adapter) {
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        try {
            return adapter.readValue(value.charStream());
        } finally {
            value.close();
        }
    }
}
