package com.hanschen.easyloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.callback.OnLoadListener;
import com.hanschen.easyloader.log.Logger;

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

        private final Context         context;
        private       boolean         logEnable;
        private       CacheManager    memoryCacheManager;
        private       CacheManager    diskCacheManager;
        private       Logger          logger;
        private       OnLoadListener  listener;
        private       ExecutorService service;
        private       Bitmap.Config   defaultBitmapConfig;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context can not be null");
            }
            this.context = context.getApplicationContext();
        }

        public EasyLoader build() {
            EasyLoader loader = new EasyLoader();
            loader.apply(Builder.this);
            return loader;
        }
    }
}
