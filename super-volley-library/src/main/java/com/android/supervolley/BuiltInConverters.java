package com.android.supervolley;

import com.android.supervolley.annotation.Streaming;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

final class BuiltInConverters extends Converter.Factory {
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations) {
        if (type == ResponseBody.class) {
            return Utils.isAnnotationPresent(annotations, Streaming.class)
                    ? StreamingResponseBodyConverter.INSTANCE
                    : BufferingResponseBodyConverter.INSTANCE;
        }
        if (type == Void.class) {
            return VoidResponseBodyConverter.INSTANCE;
        }
        return null;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations) {
        if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
            return RequestBodyConverter.INSTANCE;
        }
        return null;
    }

    static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override
        public Void convert(ResponseBody value) throws IOException {
            value.close();
            return null;
        }
    }

    static final class RequestBodyConverter implements Converter<RequestBody, RequestBody> {
        static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

        @Override
        public RequestBody convert(RequestBody value) throws IOException {
            return value;
        }
    }

    static final class StreamingResponseBodyConverter
            implements Converter<ResponseBody, ResponseBody> {
        static final StreamingResponseBodyConverter INSTANCE = new StreamingResponseBodyConverter();

        @Override
        public ResponseBody convert(ResponseBody value) throws IOException {
            return value;
        }
    }

    static final class BufferingResponseBodyConverter
            implements Converter<ResponseBody, ResponseBody> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override
        public ResponseBody convert(ResponseBody value) throws IOException {
            try {
                // Buffer the entire body to avoid future I/O.
                return Utils.buffer(value);
            } finally {
                value.close();
            }
        }
    }

    static final class ToStringConverter implements Converter<Object, String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override
        public String convert(Object value) {
            return value.toString();
        }
    }
}
