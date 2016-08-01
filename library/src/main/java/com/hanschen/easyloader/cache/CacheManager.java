package com.hanschen.easyloader.cache;

/**
 * Created by Hans.Chen on 2016/7/28.
 */
public interface CacheManager<T> {

    T get(String key);

    void set(String key, T value);

    int size();

    int maxSize();

    void clear();

}
