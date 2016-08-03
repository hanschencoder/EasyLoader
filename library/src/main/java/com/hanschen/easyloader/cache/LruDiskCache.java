package com.hanschen.easyloader.cache;

import com.hanschen.easyloader.cache.diskcache.DiskLruCache;
import com.hanschen.easyloader.log.EasyLoaderLog;

import java.io.File;
import java.io.IOException;

/**
 * Created by Hans.Chen on 2016/8/2.
 */
public class LruDiskCache implements CacheManager<String, File> {

    private static final String TAG = "LruDiskCache";

    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    private DiskLruCache diskLruCache;

    public LruDiskCache(File directory, int maxSize) {
        if (diskLruCache == null) {
            try {
                diskLruCache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize);
            } catch (IOException e) {
                EasyLoaderLog.e(TAG, "can't open DiskLruCache");
                e.printStackTrace();
            }
        }
    }

    @Override
    public File get(String key) {
        return null;
    }

    @Override
    public void put(String key, File value) {

    }

    @Override
    public File remove(String key) {
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
