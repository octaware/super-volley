package com.android.supervolley;

import com.android.supervolley.annotation.Body;
import com.android.supervolley.annotation.CacheResponse;
import com.android.supervolley.annotation.DELETE;
import com.android.supervolley.annotation.Field;
import com.android.supervolley.annotation.FieldMap;
import com.android.supervolley.annotation.FormUrlEncoded;
import com.android.supervolley.annotation.GET;
import com.android.supervolley.annotation.HEAD;
import com.android.supervolley.annotation.HTTP;
import com.android.supervolley.annotation.Header;
import com.android.supervolley.annotation.HeaderMap;
import com.android.supervolley.annotation.Multipart;
import com.android.supervolley.annotation.OPTIONS;
import com.android.supervolley.annotation.PATCH;
import com.android.supervolley.annotation.POST;
import com.android.supervolley.annotation.PUT;
import com.android.supervolley.annotation.Part;
import com.android.supervolley.annotation.PartMap;
import com.android.supervolley.annotation.Path;
import com.android.supervolley.annotation.Priority;
import com.android.supervolley.annotation.Query;
import com.android.supervolley.annotation.QueryMap;
import com.android.supervolley.annotation.Retries;
import com.android.supervolley.annotation.Tag;
import com.android.supervolley.annotation.Url;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Adapts an invocation of an interface method into an HTTP call.
 */
final class ServiceMethod<R, T> {
    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

    final CallAdapter<R, T> callAdapter;

    private final okhttp3.HttpUrl baseUrl;
    private final Converter<ResponseBody, R> responseConverter;
    private final String httpMethod;
    private final String relativeUrl;
    private final Map<String, String> headers;
    private final MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
    private final boolean shouldCache;
    private final ParameterHandler<?>[] parameterHandlers;
    private final RetryPolicy retryPolicy;
    private final Request.Priority priority;
    private final String tag;
    final RequestQueue requestQueue;

    ServiceMethod(Builder<R, T> builder) {
        this.callAdapter = builder.callAdapter;
        this.baseUrl = builder.volley.baseUrl();
        this.responseConverter = builder.responseConverter;
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.headers = builder.headers;
        this.contentType = builder.contentType;
        this.hasBody = builder.hasBody;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
        this.parameterHandlers = builder.parameterHandlers;
        this.retryPolicy = builder.retryPolicy;
        this.shouldCache = builder.shouldCache;
        this.priority = builder.priority;
        this.tag = builder.tag;
        this.requestQueue = builder.volley.requestQueue;
    }

    /**
     * Builds an HTTP request from method arguments.
     */
    Request toRequest(Object... args) throws IOException {
        RequestBuilder requestBuilder = new RequestBuilder();
        requestBuilder.method(httpMethod).baseUrl(baseUrl)
                .relativeUrl(relativeUrl).headers(headers)
                .contentType(contentType).hasBody(hasBody)
                .isFormEncoded(isFormEncoded).isMultipart(isMultipart)
                .priority(priority).retryPolicy(retryPolicy)
                .tag(tag).shouldCache(shouldCache);

        @SuppressWarnings("unchecked")
        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

        int argumentCount = args != null ? args.length : 0;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException("Argument count (" + argumentCount
                    + ") doesn't match expected count (" + handlers.length + ")");
        }

        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(requestBuilder, args[p]);
        }

        return requestBuilder.build();
    }

    /**
     * Builds a method return value from an HTTP response body.
     */
    R toResponse(ResponseBody body) throws IOException {
        return responseConverter.convert(body);
    }

    /**
     * Inspects the annotations on an interface method to construct a reusable service method. This
     * requires potentially-expensive reflection so it is best to build each service method only once
     * and reuse it. Builders cannot be reused.
     */
    static final class Builder<T, R> {
        final SuperVolley volley;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        Type responseType;
        boolean gotField;
        boolean gotPart;
        boolean gotBody;
        boolean gotPath;
        boolean gotQuery;
        boolean gotUrl;
        String httpMethod;
        boolean hasBody;
        boolean isFormEncoded;
        boolean isMultipart;
        boolean shouldCache = false;

        RetryPolicy retryPolicy;
        Request.Priority priority;
        String tag;

        String relativeUrl;
        Map<String, String> headers = Collections.emptyMap();
        MediaType contentType;
        Set<String> relativeUrlParamNames;
        ParameterHandler<?>[] parameterHandlers;
        Converter<ResponseBody, T> responseConverter;
        CallAdapter<T, R> callAdapter;

        Builder(SuperVolley volley, Method method) {
            this.volley = volley;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }

        public ServiceMethod build() {
            callAdapter = createCallAdapter();
            responseType = callAdapter.responseType();
            if (responseType == Response.class || responseType == okhttp3.Response.class) {
                throw methodError("'"
                        + Utils.getRawType(responseType).getName()
                        + "' is not a valid response body type. Did you mean ResponseBody?");
            }
            responseConverter = createResponseConverter();

            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }

            if (httpMethod == null) {
                throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }

            if (!hasBody) {
                if (isMultipart) {
                    throw methodError(
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
                            + "request body (e.g., @POST).");
                }
            }

            int parameterCount = parameterAnnotationsArray.length;
            parameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0; p < parameterCount; p++) {
                Type parameterType = parameterTypes[p];
                if (Utils.hasUnresolvableType(parameterType)) {
                    throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
                            parameterType);
                }

                Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
                if (parameterAnnotations == null) {
                    throw parameterError(p, "No SuperVolley annotation found.");
                }

                parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
            }

            if (relativeUrl == null && !gotUrl) {
                throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
            }
            if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
                throw methodError("Non-body HTTP method cannot contain @Body.");
            }
            if (isFormEncoded && !gotField) {
                throw methodError("Form-encoded method must contain at least one @Field.");
            }
            if (isMultipart && !gotPart) {
                throw methodError("Multipart method must contain at least one @Part.");
            }

            if (tag != null && tag.length() == 0) {
                throw methodError("Tag can not be empty.");
            }

            if (priority == null) {
                priority = Request.Priority.NORMAL;
            }

            if (tag == null) {
                tag = relativeUrl;
            }

            if (retryPolicy == null) {
                retryPolicy = new NoRetryPolicy();
            }

            return new ServiceMethod<>(this);
        }

        private CallAdapter<T, R> createCallAdapter() {
            Type returnType = method.getGenericReturnType();
            if (Utils.hasUnresolvableType(returnType)) {
                throw methodError(
                        "Method return type must not include a type variable or wildcard: %s", returnType);
            }
            if (returnType == void.class) {
                throw methodError("Service methods cannot return void.");
            }
            Annotation[] annotations = method.getAnnotations();
            try {
                //noinspection unchecked
                return (CallAdapter<T, R>) volley.callAdapter(returnType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create call adapter for %s", returnType);
            }
        }

        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof DELETE) {
                parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
            } else if (annotation instanceof GET) {
                parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
            } else if (annotation instanceof HEAD) {
                parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
                if (!Void.class.equals(responseType)) {
                    throw methodError("HEAD method must use Void as response type.");
                }
            } else if (annotation instanceof PATCH) {
                parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
            } else if (annotation instanceof POST) {
                parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
            } else if (annotation instanceof PUT) {
                parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
            } else if (annotation instanceof OPTIONS) {
                parseHttpMethodAndPath("OPTIONS", ((OPTIONS) annotation).value(), false);
            } else if (annotation instanceof HTTP) {
                HTTP http = (HTTP) annotation;
                parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
            } else if (annotation instanceof com.android.supervolley.annotation.Headers) {
                String[] headersToParse = ((com.android.supervolley.annotation.Headers) annotation).value();
                if (headersToParse.length == 0) {
                    throw methodError("@Headers annotation is empty.");
                }
                headers = parseHeaders(headersToParse);
            } else if (annotation instanceof Multipart) {
                if (isFormEncoded) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                isMultipart = true;
            } else if (annotation instanceof FormUrlEncoded) {
                if (isMultipart) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                isFormEncoded = true;
            } else if (annotation instanceof CacheResponse) {
                shouldCache = true;
            } else if (annotation instanceof Retries) {
                /* The default socket timeout in milliseconds */
                final int DEFAULT_TIMEOUT_MS = 2500;
                /* The default backoff multiplier */
                final float DEFAULT_BACKOFF_MULT = 1f;
                int[] retry = ((Retries) annotation).value();
                if (retry.length == 1) {
                    retryPolicy = new DefaultRetryPolicy(DEFAULT_TIMEOUT_MS, retry[0], DEFAULT_BACKOFF_MULT);
                } else if (retry.length == 2) {
                    retryPolicy = new DefaultRetryPolicy(retry[1], retry[0], DEFAULT_BACKOFF_MULT);
                } else if (retry.length == 3) {
                    retryPolicy = new DefaultRetryPolicy(retry[1], retry[0], retry[2]);
                } else {
                    retryPolicy = new NoRetryPolicy();
                }
            } else if (annotation instanceof Priority) {
                priority = ((Priority) annotation).value();
            } else if (annotation instanceof Tag) {
                tag = ((Tag) annotation).value();
            }
        }

        private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
            if (this.httpMethod != null) {
                throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
                        this.httpMethod, httpMethod);
            }
            this.httpMethod = httpMethod;
            this.hasBody = hasBody;

            if (value.isEmpty()) {
                return;
            }

            // Get the relative URL path and existing query string, if present.
            int question = value.indexOf('?');
            if (question != -1 && question < value.length() - 1) {
                // Ensure the query string does not have any named parameters.
                String queryParams = value.substring(question + 1);
                Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
                if (queryParamMatcher.find()) {
                    throw methodError("URL query string \"%s\" must not have replace block. "
                            + "For dynamic query parameters use @Query.", queryParams);
                }
            }

            this.relativeUrl = value;
            this.relativeUrlParamNames = parsePathParameters(value);
        }

        private Map<String, String> parseHeaders(String[] headers) {
            Map<String, String> map = new HashMap<>(headers.length);
            for (String header : headers) {
                int colon = header.indexOf(':');
                if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                    throw methodError(
                            "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
                }
                String headerName = header.substring(0, colon);
                String headerValue = header.substring(colon + 1).trim();
                if ("Content-Type".equalsIgnoreCase(headerName)) {
                    MediaType type = MediaType.parse(headerValue);
                    if (type == null) {
                        throw methodError("Malformed content type: %s", headerValue);
                    }
                    contentType = type;
                } else {
                    map.put(headerName, headerValue);
                }
            }
            return map;
        }

        private ParameterHandler<?> parseParameter(
                int p, Type parameterType, Annotation[] annotations) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(
                        p, parameterType, annotations, annotation);

                if (annotationAction == null) {
                    continue;
                }

                if (result != null) {
                    throw parameterError(p, "Multiple SuperVolley annotations found, only one allowed.");
                }

                result = annotationAction;
            }

            if (result == null) {
                throw parameterError(p, "No SuperVolley annotation found.");
            }

            return result;
        }

        private ParameterHandler<?> parseParameterAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Url) {
                if (gotUrl) {
                    throw parameterError(p, "Multiple @Url method annotations found.");
                }
                if (gotPath) {
                    throw parameterError(p, "@Path parameters may not be used with @Url.");
                }
                if (gotQuery) {
                    throw parameterError(p, "A @Url parameter must not come after a @Query");
                }
                if (relativeUrl != null) {
                    throw parameterError(p, "@Url cannot be used with @%s URL", httpMethod);
                }

                gotUrl = true;

                if (type == HttpUrl.class
                        || type == String.class
                        || type == URI.class
                        || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
                    return new ParameterHandler.RelativeUrl();
                } else {
                    throw parameterError(p,
                            "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
                }

            } else if (annotation instanceof Path) {
                if (gotQuery) {
                    throw parameterError(p, "A @Path parameter must not come after a @Query.");
                }
                if (gotUrl) {
                    throw parameterError(p, "@Path parameters may not be used with @Url.");
                }
                if (relativeUrl == null) {
                    throw parameterError(p, "@Path can only be used with relative url on @%s", httpMethod);
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();
                validatePathName(p, name);

                Converter<?, String> converter = volley.stringConverter(type, annotations);
                return new ParameterHandler.Path<>(name, converter, path.encoded());

            } else if (annotation instanceof Query) {
                Query query = (Query) annotation;
                String name = query.value();
                boolean encoded = query.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQuery = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            volley.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            volley.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter =
                            volley.stringConverter(type, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded);
                }

            } else if (annotation instanceof QueryMap) {
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@QueryMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@QueryMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter =
                        volley.stringConverter(valueType, annotations);

                return new ParameterHandler.QueryMap<>(valueConverter, ((QueryMap) annotation).encoded());

            } else if (annotation instanceof Header) {
                Header header = (Header) annotation;
                String name = header.value();

                Class<?> rawParameterType = Utils.getRawType(type);
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            volley.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Header<>(name, converter).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            volley.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Header<>(name, converter).array();
                } else {
                    Converter<?, String> converter =
                            volley.stringConverter(type, annotations);
                    return new ParameterHandler.Header<>(name, converter);
                }

            } else if (annotation instanceof HeaderMap) {
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@HeaderMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@HeaderMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter =
                        volley.stringConverter(valueType, annotations);

                return new ParameterHandler.HeaderMap<>(valueConverter);

            } else if (annotation instanceof Field) {
                if (!isFormEncoded) {
                    throw parameterError(p, "@Field parameters can only be used with form encoding.");
                }
                Field field = (Field) annotation;
                String name = field.value();
                boolean encoded = field.encoded();

                gotField = true;

                Class<?> rawParameterType = Utils.getRawType(type);
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            volley.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            volley.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter =
                            volley.stringConverter(type, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded);
                }

            } else if (annotation instanceof FieldMap) {
                if (!isFormEncoded) {
                    throw parameterError(p, "@FieldMap parameters can only be used with form encoding.");
                }
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@FieldMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p,
                            "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@FieldMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter =
                        volley.stringConverter(valueType, annotations);

                gotField = true;
                return new ParameterHandler.FieldMap<>(valueConverter, ((FieldMap) annotation).encoded());

            } else if (annotation instanceof Part) {
                if (!isMultipart) {
                    throw parameterError(p, "@Part parameters can only be used with multipart encoding.");
                }
                Part part = (Part) annotation;
                gotPart = true;

                String partName = part.value();
                Class<?> rawParameterType = Utils.getRawType(type);
                if (partName.isEmpty()) {
                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
                        if (!(type instanceof ParameterizedType)) {
                            throw parameterError(p, rawParameterType.getSimpleName()
                                    + " must include generic type (e.g., "
                                    + rawParameterType.getSimpleName()
                                    + "<String>)");
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                        if (!MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                            throw parameterError(p,
                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                        }
                        return ParameterHandler.RawPart.INSTANCE.iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = rawParameterType.getComponentType();
                        if (!MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw parameterError(p,
                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                        }
                        return ParameterHandler.RawPart.INSTANCE.array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        return ParameterHandler.RawPart.INSTANCE;
                    } else {
                        throw parameterError(p,
                                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                    }
                } else {
                    Headers headers =
                            Headers.of("Content-Disposition", "form-data; name=\"" + partName + "\"",
                                    "Content-Transfer-Encoding", part.encoding());

                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
                        if (!(type instanceof ParameterizedType)) {
                            throw parameterError(p, rawParameterType.getSimpleName()
                                    + " must include generic type (e.g., "
                                    + rawParameterType.getSimpleName()
                                    + "<String>)");
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                        if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                            throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
                                    + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                volley.requestBodyConverter(iterableType, annotations, methodAnnotations);
                        return new ParameterHandler.Part<>(headers, converter).iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                        if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
                                    + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                volley.requestBodyConverter(arrayComponentType, annotations, methodAnnotations);
                        return new ParameterHandler.Part<>(headers, converter).array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
                                + "include a part name in the annotation.");
                    } else {
                        Converter<?, RequestBody> converter =
                                volley.requestBodyConverter(type, annotations, methodAnnotations);
                        return new ParameterHandler.Part<>(headers, converter);
                    }
                }

            } else if (annotation instanceof PartMap) {
                if (!isMultipart) {
                    throw parameterError(p, "@PartMap parameters can only be used with multipart encoding.");
                }
                gotPart = true;
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@PartMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;

                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@PartMap keys must be of type String: " + keyType);
                }

                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
                    throw parameterError(p, "@PartMap values cannot be MultipartBody.Part. "
                            + "Use @Part List<Part> or a different value type instead.");
                }

                Converter<?, RequestBody> valueConverter =
                        volley.requestBodyConverter(valueType, annotations, methodAnnotations);

                PartMap partMap = (PartMap) annotation;
                return new ParameterHandler.PartMap<>(valueConverter, partMap.encoding());

            } else if (annotation instanceof Body) {
                if (isFormEncoded || isMultipart) {
                    throw parameterError(p,
                            "@Body parameters cannot be used with form or multi-part encoding.");
                }
                if (gotBody) {
                    throw parameterError(p, "Multiple @Body method annotations found.");
                }

                Converter<?, RequestBody> converter;
                try {
                    converter = volley.requestBodyConverter(type, annotations, methodAnnotations);
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw parameterError(e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a SuperVolley annotation.
        }

        private void validatePathName(int p, String name) {
            if (!PARAM_NAME_REGEX.matcher(name).matches()) {
                throw parameterError(p, "@Path parameter name must match %s. Found: %s",
                        PARAM_URL_REGEX.pattern(), name);
            }
            // Verify URL replacement name is actually present in the URL path.
            if (!relativeUrlParamNames.contains(name)) {
                throw parameterError(p, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
            }
        }

        private Converter<ResponseBody, T> createResponseConverter() {
            Annotation[] annotations = method.getAnnotations();
            try {
                return volley.responseBodyConverter(responseType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create converter for %s", responseType);
            }
        }

        private RuntimeException methodError(String message, Object... args) {
            return methodError(null, message, args);
        }

        private RuntimeException methodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + method.getDeclaringClass().getSimpleName()
                    + "."
                    + method.getName(), cause);
        }

        private RuntimeException parameterError(
                Throwable cause, int p, String message, Object... args) {
            return methodError(cause, message + " (parameter #" + (p + 1) + ")", args);
        }

        private RuntimeException parameterError(int p, String message, Object... args) {
            return methodError(message + " (parameter #" + (p + 1) + ")", args);
        }
    }

    /**
     * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
     * in the URI, it will only show up once in the set.
     */
    static Set<String> parsePathParameters(String path) {
        Matcher m = PARAM_URL_REGEX.matcher(path);
        Set<String> patterns = new LinkedHashSet<>();
        while (m.find()) {
            patterns.add(m.group(1));
        }
        return patterns;
    }

    static Class<?> boxIfPrimitive(Class<?> type) {
        if (boolean.class == type) return Boolean.class;
        if (byte.class == type) return Byte.class;
        if (char.class == type) return Character.class;
        if (double.class == type) return Double.class;
        if (float.class == type) return Float.class;
        if (int.class == type) return Integer.class;
        if (long.class == type) return Long.class;
        if (short.class == type) return Short.class;
        return type;
    }
}
