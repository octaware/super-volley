package com.android.supervolley.cache;

import com.android.volley.Cache;
import com.android.volley.VolleyLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache implementation that caches requests responses in memory
 * <p>
 */
public class MemoryBasedCache implements Cache {

    /**
     * Map of the Key, CacheHeader pairs
     */
    private Map<String, Entry> entries;

    /**
     * Returns the cache entry with the specified key if it exists, null otherwise.
     */
    @Override
    public Entry get(String key) {
        if (entries == null) {
            return null;
        }
        return entries.get(key);
    }

    /**
     * Puts the entry with the specified key into the cache.
     */
    @Override
    public void put(String key, Entry entry) {
        if (entries == null) {
            initialize();
        }
        entries.put(key, entry);
    }

    /**
     * Initializes the MemoryBasedCache concurrent memory map
     */
    @Override
    public void initialize() {
        this.entries = new ConcurrentHashMap<>();
    }

    /**
     * Invalidates an entry in the cache.
     *
     * @param key        Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    @Override
    public void invalidate(String key, boolean fullExpire) {
        Entry entry = get(key);
        if (entry != null) {
            entry.softTtl = 0;
            if (fullExpire) {
                entry.ttl = 0;
            }
            put(key, entry);
        }
    }

    /**
     * Removes the specified key from the cache if it exists.
     */
    @Override
    public void remove(String key) {
        entries.remove(key);
    }

    /**
     * Clears the cache. Deletes all cached files from memory.
     */
    @Override
    public void clear() {
        entries.clear();
        VolleyLog.d("CacheResponse cleared.");
    }

    /**
     * @return true if we don't have any entries in the cache
     */
    public boolean isCacheEmpty() {
        return entries == null || entries.isEmpty();
    }
}
