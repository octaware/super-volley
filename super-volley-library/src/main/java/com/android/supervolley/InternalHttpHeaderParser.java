package com.android.supervolley;


import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Map;

/**
 * Custom Http header parser. Made package private to avoid unnecessary usage.
 */
class InternalHttpHeaderParser extends HttpHeaderParser {
    private static final long DEFAULT_CACHE_TIME_IN_MILLIS = 60 * 60 * 1000;

    /**
     * @param response The network response to parse headers from
     * @return A cache entry for the given response
     */
    public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response) {
        return parseIgnoreCacheHeaders(response, -1);
    }

    /**
     * Extracts a {@link com.android.volley.Cache.Entry} from a {@link com.android.volley.NetworkResponse}.
     * Cache-control headers are ignored. SoftTtl == 3 min, ttl == 24 hours.
     *
     * @param response The network response to parse headers from
     * @return A cache entry for the given response
     */
    static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response, long cacheTimeInMillis) {
        /* Get headers to easier use */
        Map<String, String> headers = response.headers;

        /* Get the server date */
        long serverDate = 0;
        String date = headers.get("Date");
        if (date != null) {
            serverDate = parseDateAsEpoch(date);
        }

        /* Pars servers ETag values */
        final String serverETag = headers.get("ETag");

        /* Initialize caching time constrain values */
        final long now = System.currentTimeMillis();
        // Amount of time in which the cache will be hit, but also refreshed on background
        final long cacheHitButRefreshed = cacheTimeInMillis > 0 ? cacheTimeInMillis : DEFAULT_CACHE_TIME_IN_MILLIS;
        // Amount of time in which this cache entry expires completely
        final long cacheExpired = cacheTimeInMillis > 0 ? cacheTimeInMillis : DEFAULT_CACHE_TIME_IN_MILLIS;
        final long softExpire = now + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;

        /* Build cache entry */
        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverETag;
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }
}
