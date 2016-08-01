package com.hanschen.easyloader.cache;

import android.support.v4.util.LruCache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public class LruMemoryCache<T> implements CacheManager<T> {

    private final int                      maxSize;
    private       int                      hitCount;
    private       int                      missCount;
    private       int                      putCount;
    private       int                      evictionCount;
    private       int                      size;
    private final LinkedHashMap<String, T> map;
    private final ByteCalculator<T>        calculator;

    public LruMemoryCache(int maxSize, ByteCalculator<T> calculator) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
        this.calculator = calculator;
    }

    @Override
    public T get(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        T mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }
        return null;
    }

    @Override
    public void set(String key, T value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }

        int addedSize = calculator.getByteCount(value);
        if (addedSize > maxSize) {
            return;
        }

        synchronized (this) {
            putCount++;
            size += addedSize;
            T previous = map.put(key, value);
            if (previous != null) {
                size -= calculator.getByteCount(previous);
            }
        }

        trimToSize(maxSize);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public void clear() {
        trimToSize(-1); // -1 will evict 0-sized elements
        android.util.LruCache
    }

    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            T value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<String, T> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= calculator.getByteCount(value);
                evictionCount++;
            }
        }
    }
}
