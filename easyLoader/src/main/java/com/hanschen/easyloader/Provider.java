package com.hanschen.easyloader;

import android.content.Context;
import android.graphics.Bitmap;

import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.request.RequestHandler;

import java.lang.ref.ReferenceQueue;
import java.util.List;

/**
 * Created by chenhang on 2016/10/25.
 */

public interface Provider {

    Context getContext();

    ReferenceQueue<Object> getReferenceQueue();

    List<RequestHandler> getRequestHandlers();

    CacheManager<String, Bitmap> getMemoryCacheManager();

    CacheManager<String, Bitmap> getDiskCacheManager();

    Bitmap.Config getDefaultBitmapConfig();

    QueueProcessType getQueueProcessType();

    boolean isShutdown();

    boolean isIndicatorsEnabled();
}
