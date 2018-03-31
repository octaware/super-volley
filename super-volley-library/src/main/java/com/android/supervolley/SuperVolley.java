package com.android.supervolley;

import android.content.Context;

import com.android.supervolley.annotation.GET;
import com.android.supervolley.annotation.HTTP;
import com.android.supervolley.annotation.Header;
import com.android.supervolley.annotation.Url;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.NoCache;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import static java.util.Collections.unmodifiableList;

/**
 * SuperVolley adapts a Java interface to HTTP calls by using annotations on the declared methods to
 * define how requests are made. Create instances using {@linkplain Builder
 * the builder} and pass your interface to {@link #create} to generate an implementation.
 * <p>
 * For example,
 * <pre><code>
 * SuperVolley volley = new SuperVolley.Builder()
 *     .baseUrl("https://api.example.com/")
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build();
 *
 * MyApi api = volley.create(MyApi.class);
 * Response&lt;User&gt; user = api.getUser().execute();
 * </code></pre>
 */
public final class SuperVolley {
    private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();

    final okhttp3.HttpUrl baseUrl;
    final List<Converter.Factory> converterFactories;
    final List<CallAdapter.Factory> adapterFactories;
    final Executor callbackExecutor;
    final boolean validateEagerly;
    final RequestQueue requestQueue;
    private int timeOut;

    SuperVolley(RequestQueue requestQueue, okhttp3.HttpUrl baseUrl,
                List<Converter.Factory> converterFactories, List<CallAdapter.Factory> adapterFactories,
                Executor callbackExecutor, boolean validateEagerly, int timeOut) {
        this.baseUrl = baseUrl;
        this.converterFactories = unmodifiableList(converterFactories); // Defensive copy at call site.
        this.adapterFactories = unmodifiableList(adapterFactories); // Defensive copy at call site.
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
        this.requestQueue = requestQueue;
        this.requestQueue.start();
        this.timeOut = timeOut;
    }

    /**
     * Create an implementation of the API endpoints defined by the {@code service} interface.
     * <p>
     * The relative path for a given method is obtained from an annotation on the method describing
     * the request type. The built-in methods are {@link com.android.supervolley.annotation.GET GET},
     * {@link com.android.supervolley.annotation.PUT PUT}, {@link com.android.supervolley.annotation.POST POST}, {@link com.android.supervolley.annotation.PATCH
     * PATCH}, {@link com.android.supervolley.annotation.HEAD HEAD}, {@link com.android.supervolley.annotation.DELETE DELETE} and
     * {@link com.android.supervolley.annotation.OPTIONS OPTIONS}. You can use a custom HTTP method with
     * {@link HTTP @HTTP}. For a dynamic URL, omit the path on the annotation and annotate the first
     * parameter with {@link Url @Url}.
     * <p>
     * Method parameters can be used to replace parts of the URL by annotating them with
     * {@link com.android.supervolley.annotation.Path @Path}. Replacement sections are denoted by an identifier
     * surrounded by curly braces (e.g., "{foo}"). To add items to the query string of a URL use
     * {@link com.android.supervolley.annotation.Query @Query}.
     * <p>
     * The body of a request is denoted by the {@link com.android.supervolley.annotation.Body @Body} annotation. The
     * object will be converted to request representation by one of the {@link Converter.Factory}
     * instances. A {@link RequestBody} can also be used for a raw representation.
     * <p>
     * Alternative request body formats are supported by method annotations and corresponding
     * parameter annotations:
     * <ul>
     * <li>{@link com.android.supervolley.annotation.FormUrlEncoded @FormUrlEncoded} - Form-encoded data with key-value
     * pairs specified by the {@link com.android.supervolley.annotation.Field @Field} parameter annotation.
     * <li>{@link com.android.supervolley.annotation.Multipart @Multipart} - RFC 2388-compliant multipart data with
     * parts specified by the {@link com.android.supervolley.annotation.Part @Part} parameter annotation.
     * </ul>
     * <p>
     * Additional static headers can be added for an endpoint using the
     * {@link com.android.supervolley.annotation.Headers @Headers} method annotation. For per-request control over a
     * header annotate a parameter with {@link Header @Header}.
     * <p>
     * By default, methods return a {@link Call} which represents the HTTP request. The generic
     * parameter of the call is the response body type and will be converted by one of the
     * {@link Converter.Factory} instances. {@link ResponseBody} can also be used for a raw
     * representation. {@link Void} can be used if you do not care about the body contents.
     * <p>
     * For example:
     * <pre>
     * public interface CategoryService {
     *   &#64;POST("category/{cat}/")
     *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    public <T> T create(final Class<T> service) {
        Utils.validateServiceInterface(service);
        if (validateEagerly) {
            eagerlyValidateMethods(service);
        }
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {
                    private final Platform platform = Platform.get();

                    @Override
                    public Object invoke(Object proxy, Method method, Object... args)
                            throws Throwable {
                        // If the method is a method from Object then defer to normal invocation.
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        if (platform.isDefaultMethod(method)) {
                            return platform.invokeDefaultMethod(method, service, proxy, args);
                        }
                        ServiceMethod<Object, Object> serviceMethod =
                                (ServiceMethod<Object, Object>) loadServiceMethod(method);
                        VolleyCall<Object> call = new VolleyCall<>(serviceMethod, args, requestQueue, timeOut);
                        return serviceMethod.callAdapter.adapt(call);
                    }
                });
    }

    private void eagerlyValidateMethods(Class<?> service) {
        Platform platform = Platform.get();
        for (Method method : service.getDeclaredMethods()) {
            if (!platform.isDefaultMethod(method)) {
                loadServiceMethod(method);
            }
        }
    }

    ServiceMethod<?, ?> loadServiceMethod(Method method) {
        ServiceMethod<?, ?> result = serviceMethodCache.get(method);
        if (result != null) return result;

        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                result = new ServiceMethod.Builder<>(this, method).build();
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }

    /**
     * The API base URL.
     */
    public okhttp3.HttpUrl baseUrl() {
        return baseUrl;
    }

    /**
     * Returns a list of the factories tried when creating a
     * {@linkplain #callAdapter(Type, Annotation[])} call adapter}.
     */
    public List<CallAdapter.Factory> callAdapterFactories() {
        return adapterFactories;
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    public CallAdapter<?, ?> nextCallAdapter(CallAdapter.Factory skipPast, Type returnType,
                                             Annotation[] annotations) {
        Utils.checkNotNull(returnType, "returnType == null");
        Utils.checkNotNull(annotations, "annotations == null");

        int start = adapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = adapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = adapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a list of the factories tried when creating a
     * {@linkplain #requestBodyConverter(Type, Annotation[], Annotation[]) request body converter}, a
     * {@linkplain #responseBodyConverter(Type, Annotation[]) response body converter}, or a
     * {@linkplain #stringConverter(Type, Annotation[]) string converter}.
     */
    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<T, RequestBody> requestBodyConverter(Type type,
                                                              Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<T, RequestBody> nextRequestBodyConverter(Converter.Factory skipPast,
                                                                  Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        Utils.checkNotNull(type, "type == null");
        Utils.checkNotNull(parameterAnnotations, "parameterAnnotations == null");
        Utils.checkNotNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, RequestBody>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<ResponseBody, T> nextResponseBodyConverter(Converter.Factory skipPast,
                                                                    Type type, Annotation[] annotations) {
        Utils.checkNotNull(type, "type == null");
        Utils.checkNotNull(annotations, "annotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<ResponseBody, T>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link String} from the available
     * {@linkplain #converterFactories() factories}.
     */
    public <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        Utils.checkNotNull(type, "type == null");
        Utils.checkNotNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    /**
     * The executor used for {@link Callback} methods on a {@link Call}. This may be {@code null},
     * in which case callbacks should be made synchronously on the background thread.
     */
    public Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Build a new {@link SuperVolley}.
     * <p>
     * Calling {@link #baseUrl} is required before calling {@link #build()}. All other methods
     * are optional.
     */
    public static final class Builder {

        /**
         * The default caching directory name
         */
        private static final String DEFAULT_CACHE_DIR_NAME = "SuperVolley";
        private final Platform platform;
        private okhttp3.Call.Factory callFactory;
        private okhttp3.HttpUrl baseUrl;
        private Cache cache;
        private RequestQueue requestQueue;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private final List<CallAdapter.Factory> adapterFactories = new ArrayList<>();
        private Executor callbackExecutor;
        private boolean validateEagerly;
        private boolean isSecured = false;
        private boolean followRedirects = true;
        private boolean followSslRedirects = true;
        private String[] publicKeys;
        private int threadPoolSize = 5;
        private final Set<Interceptor> interceptors = new HashSet<>();
        private LogLevel logLevel;
        private int timeOut = 10;

        Builder(Platform platform) {
            this.platform = platform;
            // Add the built-in converter factory first. This prevents overriding its behavior but also
            // ensures correct behavior when using converters that consume all types.
            converterFactories.add(new BuiltInConverters());
        }

        public Builder() {
            this(Platform.get());
        }

        Builder(SuperVolley volley) {
            platform = Platform.get();
            baseUrl = volley.baseUrl;
            converterFactories.addAll(volley.converterFactories);
            adapterFactories.addAll(volley.adapterFactories);
            // Remove the default, platform-aware call adapter added by build().
            adapterFactories.remove(adapterFactories.size() - 1);
            callbackExecutor = volley.callbackExecutor;
            validateEagerly = volley.validateEagerly;
            requestQueue = volley.requestQueue;
        }

        /**
         * The HTTP client used for requests.
         * <p>
         * This is a convenience method for calling {@link #callFactory}.
         */
        public Builder client(OkHttpClient client) {
            return callFactory(Utils.checkNotNull(client, "client == null"));
        }

        /**
         * Specify a custom call factory for creating {@link Call} instances.
         * <p>
         * Note: Calling {@link #client} automatically sets this value.
         */
        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = Utils.checkNotNull(factory, "factory == null");
            return this;
        }

        /**
         * Initialize {@link SuperVolley} with custom cache implementation
         * Build request queue and start it.
         *
         * @param cache Cache Implementation
         */
        public Builder cache(Cache cache) {
            this.cache = Utils.checkNotNull(cache, "cache == null");
            return this;
        }

        /**
         * Initialize {@link SuperVolley} with a disk base cache
         *
         * @param context      Application context to get access to the applications cache directory
         * @param cacheDirName The name of {@link SuperVolley}'s cache directory
         */
        public Builder cache(Context context, String cacheDirName) {
            final File cacheDirPath = context.getCacheDir();
            final File cacheDir = new File(cacheDirPath, cacheDirName);
            cache(new DiskBasedCache(cacheDir));
            return this;
        }

        /**
         * Initialize {@link SuperVolley} with a disk base cache
         *
         * @param context             Application context to get access to the applications cache directory
         * @param cacheDirName        The name of {@link SuperVolley}'s cache directory
         * @param maxCacheSizeInBytes The maximum size of the cache in bytes.
         */
        public Builder cache(Context context, String cacheDirName, int maxCacheSizeInBytes) {
            final File cacheDirPath = context.getCacheDir();
            final File cacheDir = new File(cacheDirPath, cacheDirName);
            cache(new DiskBasedCache(cacheDir, maxCacheSizeInBytes));
            return this;
        }


        /**
         * Initialize {@link SuperVolley} with a disk base cache with a default name for the cache directory
         *
         * @param context Application context to get access to the applications cache directory
         */
        public Builder cache(Context context) {
            cache(context, DEFAULT_CACHE_DIR_NAME);
            return this;
        }

        /**
         * Set the API base URL.
         *
         * @see #baseUrl(okhttp3.HttpUrl)
         */
        public Builder baseUrl(String baseUrl) {
            Utils.checkNotNull(baseUrl, "baseUrl == null");
            okhttp3.HttpUrl httpUrl = okhttp3.HttpUrl.parse(baseUrl);
            if (httpUrl == null) {
                throw new IllegalArgumentException("Illegal URL: " + baseUrl);
            }
            return baseUrl(httpUrl);
        }

        /**
         * Set the API base URL.
         * <p>
         * The specified endpoint values (such as with {@link GET @GET}) are resolved against this
         * value using {@link okhttp3.HttpUrl#resolve(String)}. The behavior of this matches that of an
         * {@code <a href="">} link on a website resolving on the current URL.
         * <p>
         * <b>Base URLs should always end in {@code /}.</b>
         * <p>
         * A trailing {@code /} ensures that endpoints values which are relative paths will correctly
         * append themselves to a base which has path components.
         * <p>
         * <b>Correct:</b><br>
         * Base URL: http://example.com/api/<br>
         * Endpoint: foo/bar/<br>
         * Result: http://example.com/api/foo/bar/
         * <p>
         * <b>Incorrect:</b><br>
         * Base URL: http://example.com/api<br>
         * Endpoint: foo/bar/<br>
         * Result: http://example.com/foo/bar/
         * <p>
         * This method enforces that {@code baseUrl} has a trailing {@code /}.
         * <p>
         * <b>Endpoint values which contain a leading {@code /} are absolute.</b>
         * <p>
         * Absolute values retain only the host from {@code baseUrl} and ignore any specified path
         * components.
         * <p>
         * Base URL: http://example.com/api/<br>
         * Endpoint: /foo/bar/<br>
         * Result: http://example.com/foo/bar/
         * <p>
         * Base URL: http://example.com/<br>
         * Endpoint: /foo/bar/<br>
         * Result: http://example.com/foo/bar/
         * <p>
         * <b>Endpoint values may be a full URL.</b>
         * <p>
         * Values which have a host replace the host of {@code baseUrl} and values also with a scheme
         * replace the scheme of {@code baseUrl}.
         */
        public Builder baseUrl(okhttp3.HttpUrl baseUrl) {
            Utils.checkNotNull(baseUrl, "baseUrl == null");
            List<String> pathSegments = baseUrl.pathSegments();
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
            }
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Add converter factory for serialization and deserialization of objects.
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(Utils.checkNotNull(factory, "factory == null"));
            return this;
        }

        /**
         * Add a call adapter factory for supporting service method return types other than {@link
         * Call}.
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            adapterFactories.add(Utils.checkNotNull(factory, "factory == null"));
            return this;
        }

        /**
         * The executor on which {@link Callback} methods are invoked when returning {@link Call} from
         * your service method.
         * <p>
         * Note: {@code executor} is not used for {@linkplain #addCallAdapterFactory custom method
         * return types}.
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = Utils.checkNotNull(executor, "executor == null");
            return this;
        }

        /**
         * When calling {@link #create} on the resulting {@link SuperVolley} instance, eagerly validate
         * the configuration of all methods in the supplied interface.
         */
        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        public Builder secure(boolean isSecured) {
            this.isSecured = isSecured;
            return this;
        }

        /**
         * Configure the default http client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
         * <p>
         * <p>If unset, protocol redirects will be followed. This is different than the built-in {@code
         * HttpURLConnection}'s default.
         */
        public Builder followSslRedirects(boolean followProtocolRedirects) {
            this.followSslRedirects = followProtocolRedirects;
            return this;
        }

        /**
         * Configure the default http client to follow redirects. If unset, redirects be followed.
         */
        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder publicKeys(String[] publicKeys) {
            this.publicKeys = publicKeys;
            return this;
        }

        /**
         * @param threadPoolSize Number of network dispatcher threads to create
         */
        public Builder threadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        /**
         * Observes, modifies, and potentially short-circuits requests going out and the corresponding
         * responses coming back in. Typically interceptors add, remove, or transform headers on the request
         * or response.
         */
        public Builder interceptor(Interceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        /**
         * Library logger.
         */
        public Builder logLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * @param timeOut is a timeOut for the Observable (synchronized) request
         *<p>
         * The timeOut is in seconds
         */
        public Builder timeOut(int timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        /**
         * Create the {@link SuperVolley} instance using the configured values.
         * <p>
         * Note: If neither {@link #client} nor {@link #callFactory} is called a default {@link
         * OkHttpClient} will be created and used.
         */
        public SuperVolley build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }

            if (cache == null) {
                cache = new NoCache();
            }

            if (logLevel == null) {
                logLevel = LogLevel.NONE;
            }

            okhttp3.Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                if (isSecured && publicKeys == null) {
                    this.publicKeys = new String[0];
                } else if (!isSecured && publicKeys != null) {
                    throw new IllegalStateException("Please set the secure flag to true.");
                }
                callFactory = getDefaultOkHttpClient(isSecured, followRedirects, followSslRedirects,
                        publicKeys, interceptors, logLevel);
            }

            Executor callbackExecutor = this.callbackExecutor;
            if (callbackExecutor == null) {
                callbackExecutor = platform.defaultCallbackExecutor();
            }

            final Network network = new BasicNetwork(new OkHttp3Stack(callFactory));
            requestQueue = new RequestQueue(cache, network, threadPoolSize);

            // Make a defensive copy of the adapters and add the default Call adapter.
            List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
            adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

            // Make a defensive copy of the converters.
            List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

            return new SuperVolley(requestQueue, baseUrl, converterFactories,
                    adapterFactories, callbackExecutor, validateEagerly, timeOut);
        }

        /*
        * Returns OkHttpClient object with its sslSocketFactory and trustManager
        */
        private OkHttpClient getDefaultOkHttpClient(boolean isSecured, boolean followRedirects,
                                                    boolean followProtocolRedirects, String[] publicKeys,
                                                    Collection<Interceptor> interceptors, LogLevel logLevel) {
            ClientSSLSocketFactory.setIsSecured(isSecured);
            ClientSSLSocketFactory.setPublicKeys(publicKeys);
            SSLSocketFactory sslSocketFactory = ClientSSLSocketFactory.getSocketFactory();
            X509TrustManager trustManager = ClientSSLSocketFactory.get509TrustManager();
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustManager);
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(logLevel);
            builder.addInterceptor(loggingInterceptor);
            for (Interceptor interceptor : interceptors) {
                builder.addInterceptor(interceptor);
            }
            builder.followRedirects(followRedirects);
            builder.followSslRedirects(followProtocolRedirects);
            return builder.build();
        }
    }
}
