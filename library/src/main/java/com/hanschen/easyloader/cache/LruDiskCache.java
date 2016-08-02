package com.hanschen.easyloader.cache;

/**
 * Created by Hans.Chen on 2016/8/2.
 */
public class LruDiskCache<K, V> implements CacheManager<K, V> {

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {

    }

    @Override
    public V remove(K key) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void resize(int maxSize) {

    }

    @Override
    public int maxSize() {
        return 0;
    }

    @Override
    public void clear() {

    }
}
