package com.hanschen.easyloader.cache;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public interface ByteCalculator<T> {

    int getByteCount(T value);

}
