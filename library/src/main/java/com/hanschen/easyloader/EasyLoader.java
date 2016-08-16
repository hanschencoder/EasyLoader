package com.hanschen.easyloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.cache.LruDiskCache;
import com.hanschen.easyloader.cache.LruMemoryCache;
import com.hanschen.easyloader.cache.SizeCalculator;
import com.hanschen.easyloader.callback.OnLoadListener;
import com.hanschen.easyloader.log.Logger;
import com.hanschen.easyloader.util.AppUtils;
import com.hanschen.easyloader.util.BitmapUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Created by Hans.Chen on 2016/7/27.
 */
public class EasyLoader {

    private volatile static EasyLoader singleton;

    private EasyLoader() {

    }

    public static EasyLoader with(@NonNull Context context) {
        if (singleton == null) {
            synchronized (EasyLoader.class) {
                if (singleton == null) {
                    singleton = new Builder(context).build();
                }
            }
        }
        return singleton;
    }

    private void apply(final Builder builder) {
    }

    static class Builder {

        private static final long DEFAULT_DISK_CACHE_SIZE   = 100 * 1024 * 1024;
        private static final long DEFAULT_MEMORY_CACHE_SIZE = 250 * 1024 * 1024;

        private final Context                      context;
        private       boolean                      logEnable;
        private       CacheManager<String, Bitmap> memoryCacheManager;
        private       CacheManager<String, Bitmap> diskCacheManager;
        private       File                         cacheDirectory;
        private       Logger                       logger;
        private       OnLoadListener               listener;
        private       ExecutorService              service;
        private       Bitmap.Config                defaultBitmapConfig;
        private long maxMemoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE;
        private long maxDiskCacheSize   = DEFAULT_DISK_CACHE_SIZE;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context can not be null");
            }
            this.context = context.getApplicationContext();
        }

        public EasyLoader build() {
            if (memoryCacheManager == null) {
                memoryCacheManager = new LruMemoryCache<>(maxMemoryCacheSize, new SizeCalculator<Bitmap>() {
                    @Override
                    public int getSizeOf(Bitmap value) {
                        return BitmapUtils.getBitmapBytes(value);
                    }
                });
            }
            if (diskCacheManager == null) {
                if (cacheDirectory == null) {
                    File directory = context.getExternalCacheDir();
                    if (directory != null) {
                        cacheDirectory = new File(directory, "easy_loader_disk_cache");
                    }
                }
                if (cacheDirectory != null) {
                    if (cacheDirectory.mkdirs() || (cacheDirectory.exists() && cacheDirectory.isDirectory())) {
                        diskCacheManager = new LruDiskCache<>(cacheDirectory, maxDiskCacheSize, AppUtils.getVersionCode(context), new LruDiskCache.FileConverter<Bitmap>() {
                            @Override
                            public Bitmap fileToValue(File file) {
                                // TODO: 2016/8/16  
                                return null;
                            }

                            @Override
                            public boolean writeValue(Bitmap value, File to) {
                                // TODO: 2016/8/16  
                                return false;
                            }
                        });
                    }

                }
            }


            EasyLoader loader = new EasyLoader();
            loader.apply(Builder.this);
            return loader;
        }
    }
}
