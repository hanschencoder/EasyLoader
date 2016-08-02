package com.hanschen.easyloader.cache;

/**
 * Created by Hans.Chen on 2016/7/28.
 */
public interface CacheManager<K, V> {

    V get(K key);

    void put(K key, V value);

    V remove(K key);

    int size();

    void resize(int maxSize);

    int maxSize();

    void clear();

}
