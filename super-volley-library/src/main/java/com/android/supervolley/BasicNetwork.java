package com.android.supervolley;

import android.os.SystemClock;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Headers;

/**
 * A network performing Volley requests over an {@link OkHttpStack}.
 */
class BasicNetwork implements Network {

    private static final boolean DEBUG;
    private static int SLOW_REQUEST_THRESHOLD_MS;
    private final OkHttpStack mHttpStack;

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1123 format.
     */
    private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    static {
        DEBUG = VolleyLog.DEBUG;
        SLOW_REQUEST_THRESHOLD_MS = 3000;
    }

    BasicNetwork(OkHttpStack httpStack) {
        this.mHttpStack = httpStack;
    }

    public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        long requestStart = SystemClock.elapsedRealtime();

        while (true) {
            okhttp3.Response okHttpResponse = null;
            byte[] responseContents = null;
            Map<String, String> responseHeaders = Collections.emptyMap();

            try {
                // Gather headers.
                HashMap<String, String> headers = new HashMap<>();
                this.addCacheHeaders(headers, request.getCacheEntry());
                okHttpResponse = this.mHttpStack.performRequest(request, headers);

                int statusCode = okHttpResponse.code();
                responseHeaders = convertHeaders(okHttpResponse.headers());

                // Handle cache validation.
                if (statusCode == 304) { // NOT MODIFIED
                    Cache.Entry entry = request.getCacheEntry();
                    if (entry == null) {
                        return new NetworkResponse(304, null,
                                responseHeaders, true,
                                SystemClock.elapsedRealtime() - requestStart);
                    }
                    // A HTTP 304 response does not have all header fields. We
                    // have to use the header fields from the cache entry plus
                    // the new ones from the response.
                    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
                    entry.responseHeaders.putAll(responseHeaders);
                    return new NetworkResponse(304, entry.data, responseHeaders,
                            true, SystemClock.elapsedRealtime() - requestStart);
                }

                // Some responses such as 204s do not have content.  We must check.
                if (okHttpResponse.body() != null) {
                    responseContents = okHttpResponse.body().bytes();
                } else {
                    // Add 0 byte response as a way of honestly representing a no-content request.
                    responseContents = new byte[0];
                }

                // if the request is slow, log it.
                long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
                this.logSlowRequests(requestLifetime, request, responseContents, statusCode);

                if (statusCode < 200 || statusCode > 299) {
                    throw new IOException();
                }

                return new NetworkResponse(statusCode, responseContents, responseHeaders,
                        false, SystemClock.elapsedRealtime() - requestStart);
            } catch (SocketTimeoutException e) {
                attemptRetryOnException("socket", request, new TimeoutError());
            } catch (ConnectTimeoutException e) {
                attemptRetryOnException("connection", request, new TimeoutError());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            } catch (IOException e) {
                NetworkResponse networkResponse;
                if (okHttpResponse == null) {
                    throw new NoConnectionError(e);
                }
                int statusCode = okHttpResponse.code();
                // Only show error message if logging is enabled
                if (VolleyLog.DEBUG) {
                    VolleyLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());
                }

                if (responseContents != null) {
                    networkResponse = new NetworkResponse(statusCode, responseContents,
                            responseHeaders, false, SystemClock.elapsedRealtime() - requestStart);
                    if (statusCode == 401 || statusCode == 403) { // UNAUTHORIZED OR FORBIDDEN
                        attemptRetryOnException("auth", request, new AuthFailureError(networkResponse));
                    } else {
                        throw new ServerError(networkResponse);
                    }
                } else {
                    networkResponse = new NetworkResponse(statusCode, new byte[0], responseHeaders, false);
                    throw new NetworkError(networkResponse);
                }
            }
        }
    }

    private void logSlowRequests(long requestLifetime, Request<?> request, byte[] responseContents, int status) {
        if (DEBUG && requestLifetime > (long) SLOW_REQUEST_THRESHOLD_MS) {
            VolleyLog.d("HTTP response for request=<%s> [lifetime=%d], [size=%s], [rc=%d], [retryCount=%s]",
                    request, requestLifetime, responseContents != null ? Integer.valueOf(responseContents.length) : "null",
                    status, request.getRetryPolicy().getCurrentRetryCount());
        }
    }

    private static void attemptRetryOnException(String logPrefix, Request<?> request, VolleyError exception) throws VolleyError {
        RetryPolicy retryPolicy = request.getRetryPolicy();
        int oldTimeout = request.getTimeoutMs();

        try {
            retryPolicy.retry(exception);
        } catch (VolleyError var6) {
            request.addMarker(String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
            throw var6;
        }

        request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
    }

    private void addCacheHeaders(Map<String, String> headers, Cache.Entry entry) {
        if (entry != null) {
            if (entry.etag != null) {
                headers.put("If-None-Match", entry.etag);
            }

            if (entry.serverDate > 0L) {
                Date refTime = new Date(entry.serverDate);
                headers.put("If-Modified-Since", formatDate(refTime, PATTERN_RFC1123));
            }

        }
    }

    private static String formatDate(Date date, String pattern) {
        if (date == null) throw new IllegalArgumentException("date is null");
        if (pattern == null) throw new IllegalArgumentException("pattern is null");

        SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
        return formatter.format(date);
    }

    private static Map<String, String> convertHeaders(Headers headers) {
        HashMap<String, String> result = new HashMap<>();

        for (int i = 0; i < headers.size(); ++i) {
            result.put(headers.name(i), headers.value(i));
        }

        return result;
    }

    /**
     * A factory for {@link SimpleDateFormat}s. The instances are stored in a
     * threadlocal way because SimpleDateFormat is not threadsafe as noted in
     * {@link SimpleDateFormat its javadoc}.
     *
     * @author Daniel Mueller
     */
    private final static class DateFormatHolder {
        private static final ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>
                THREADLOCAL_FORMATS = new ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>() {
            @Override
            protected SoftReference<Map<String, SimpleDateFormat>> initialValue() {
                return new SoftReference<Map<String, SimpleDateFormat>>(
                        new HashMap<String, SimpleDateFormat>());
            }

        };

        /**
         * creates a {@link SimpleDateFormat} for the requested format string.
         *
         * @param pattern a non-<code>null</code> format String according to
         *                {@link SimpleDateFormat}. The format is not checked against
         *                <code>null</code>
         * @return the requested format. This simple date format should not be used
         * to {@link SimpleDateFormat#applyPattern(String) apply} to a
         * different pattern.
         */
        static SimpleDateFormat formatFor(String pattern) {
            SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
            Map<String, SimpleDateFormat> formats = ref.get();
            if (formats == null) {
                formats = new HashMap<>();
                THREADLOCAL_FORMATS.set(
                        new SoftReference<>(formats));
            }
            SimpleDateFormat format = formats.get(pattern);
            if (format == null) {
                format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                formats.put(pattern, format);
            }
            return format;
        }
    }
}
