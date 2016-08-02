package com.hanschen.easyloader.cache;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public interface SizeCalculator<T> {

    int getSizeOf(T value);

}
