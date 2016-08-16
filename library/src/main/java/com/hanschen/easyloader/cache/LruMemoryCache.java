package com.hanschen.easyloader.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Hans.Chen on 2016/8/15.
 */
public class LruMemoryCache<K, V> implements CacheManager<K, V> {

    private       long                maxSize;
    private       int                 hitCount;
    private       int                 missCount;
    private       int                 putCount;
    private       int                 evictionCount;
    private       long                size;
    private final LinkedHashMap<K, V> map;
    private final SizeCalculator<V>   calculator;

    public LruMemoryCache(int maxSize, SizeCalculator<V> calculator) {
        if (maxSize <= 0 || calculator == null) {
            throw new IllegalArgumentException("maxSize <= 0 || calculator == null");
        }
        this.maxSize = maxSize;
        //attention, accessOrder must be true
        this.map = new LinkedHashMap<>(0, 0.75f, true);
        this.calculator = calculator;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
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
    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        int addedSize = calculator.getSizeOf(value);
        if (addedSize > maxSize) {
            return;
        }

        synchronized (this) {
            putCount++;
            size += addedSize;
            V previous = map.put(key, value);
            if (previous != null) {
                size -= calculator.getSizeOf(previous);
            }
        }

        trimToSize(maxSize);
    }

    @Override
    public V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= calculator.getSizeOf(previous);
            }
        }
        return previous;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }

    @Override
    public long maxSize() {
        return maxSize;
    }

    @Override
    public void clear() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    private void trimToSize(long maxSize) {
        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= calculator.getSizeOf(value);
                evictionCount++;
            }
        }
    }
}
