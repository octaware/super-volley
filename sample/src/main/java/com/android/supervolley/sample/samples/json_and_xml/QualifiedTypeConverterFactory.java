package com.android.supervolley.sample.samples.json_and_xml;


import com.android.supervolley.Converter;
import com.android.supervolley.sample.samples.json_and_xml.annotations.Json;
import com.android.supervolley.sample.samples.json_and_xml.annotations.Xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

class QualifiedTypeConverterFactory extends Converter.Factory {

    private final Converter.Factory jsonFactory;
    private final Converter.Factory xmlFactory;

    QualifiedTypeConverterFactory(Converter.Factory jsonFactory, Converter.Factory xmlFactory) {
        this.jsonFactory = jsonFactory;
        this.xmlFactory = xmlFactory;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Json) {
                return jsonFactory.responseBodyConverter(type, annotations);
            }
            if (annotation instanceof Xml) {
                return xmlFactory.responseBodyConverter(type, annotations);
            }
        }
        return null;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations) {
        for (Annotation annotation : parameterAnnotations) {
            if (annotation instanceof Json) {
                return jsonFactory.requestBodyConverter(type, parameterAnnotations);
            }
            if (annotation instanceof Xml) {
                return xmlFactory.requestBodyConverter(type, parameterAnnotations);
            }
        }
        return null;
    }
}
