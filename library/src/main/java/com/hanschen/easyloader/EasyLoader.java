package com.hanschen.easyloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.cache.LruDiskCache;
import com.hanschen.easyloader.cache.LruMemoryCache;
import com.hanschen.easyloader.cache.SizeCalculator;
import com.hanschen.easyloader.callback.OnLoadListener;
import com.hanschen.easyloader.downloader.Downloader;
import com.hanschen.easyloader.log.Logger;
import com.hanschen.easyloader.request.NetworkRequestHandler;
import com.hanschen.easyloader.request.RequestCreator;
import com.hanschen.easyloader.request.RequestHandler;
import com.hanschen.easyloader.util.AppUtils;
import com.hanschen.easyloader.util.BitmapUtils;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by Hans.Chen on 2016/7/27.
 */
public class EasyLoader {

    private volatile static EasyLoader             singleton;
    private                 Context                context;
    private                 Bitmap.Config          bitmapConfig;
    final                   ReferenceQueue<Object> referenceQueue;
    volatile                boolean                loggingEnabled;
    final                   Dispatcher             dispatcher;
    private final           List<RequestHandler>   requestHandlers;

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    private EasyLoader(Context context, Dispatcher dispatcher, List<RequestHandler> extraRequestHandlers, Stats stats,) {

        int builtInHandlers = 7; // Adjust this as internal handlers are added or removed.
        int extraCount = (extraRequestHandlers != null ? extraRequestHandlers.size() : 0);
        List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers + extraCount);

        // ResourceRequestHandler needs to be the first in the list to avoid
        // forcing other RequestHandlers to perform null checks on request.uri
        // to cover the (request.resourceId != 0) case.
        if (extraRequestHandlers != null) {
            allRequestHandlers.addAll(extraRequestHandlers);
        }
        allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader, stats));
        requestHandlers = Collections.unmodifiableList(allRequestHandlers);

        dispatcher = new Dispatcher(context, service, HANDLER, downloader, cache, stats);
        this.referenceQueue = new ReferenceQueue<Object>();
        this.cleanupThread = new CleanupThread(referenceQueue, HANDLER);
        this.cleanupThread.start();
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public ReferenceQueue<Object> getReferenceQueue() {
        return referenceQueue;
    }

    static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Dispatcher.HUNTER_BATCH_COMPLETE: {
                    @SuppressWarnings("unchecked") List<BitmapHunter> batch = (List<BitmapHunter>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, n = batch.size(); i < n; i++) {
                        BitmapHunter hunter = batch.get(i);
                        hunter.loader.complete(hunter);
                    }
                    break;
                }
                case Dispatcher.REQUEST_GCED: {
                    Action action = (Action) msg.obj;
                    if (action.getPicasso().loggingEnabled) {
                        log(OWNER_MAIN, VERB_CANCELED, action.request.logId(), "target got garbage collected");
                    }
                    action.picasso.cancelExistingRequest(action.getTarget());
                    break;
                }
                case Dispatcher.REQUEST_BATCH_RESUME:
                    @SuppressWarnings("unchecked") List<Action> batch = (List<Action>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, n = batch.size(); i < n; i++) {
                        Action action = batch.get(i);
                        action.picasso.resumeAction(action);
                    }
                    break;
                default:
                    throw new AssertionError("Unknown handler message received: " + msg.what);
            }
        }
    };

    private static class CleanupThread extends Thread {
        private final ReferenceQueue<Object> referenceQueue;
        private final Handler                handler;

        CleanupThread(ReferenceQueue<Object> referenceQueue, Handler handler) {
            this.referenceQueue = referenceQueue;
            this.handler = handler;
            setDaemon(true);
            setName("EasyLoader-refQueue");
        }

        @Override
        public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    // Prior to Android 5.0, even when there is no local variable, the result from
                    // remove() & obtainMessage() is kept as a stack local variable.
                    // We're forcing this reference to be cleared and replaced by looping every second
                    // when there is nothing to do.
                    // This behavior has been tested and reproduced with heap dumps.
                    Action.RequestWeakReference<?> remove = (Action.RequestWeakReference<?>) referenceQueue.remove(1000);
                    Message message = handler.obtainMessage();
                    if (remove != null) {
                        message.what = Dispatcher.REQUEST_GCED;
                        message.obj = remove.action;
                        handler.sendMessage(message);
                    } else {
                        message.recycle();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException(e);
                        }
                    });
                    break;
                }
            }
        }

        void shutdown() {
            interrupt();
        }
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

    public Context getContext() {
        return context;
    }

    public Bitmap.Config getBitmapConfig() {
        return bitmapConfig;
    }

    public RequestCreator load(Uri uri) {
        return null;
    }

    static class Builder {

        private static final long DEFAULT_DISK_CACHE_SIZE   = 100 * 1024 * 1024;
        private static final long DEFAULT_MEMORY_CACHE_SIZE = 250 * 1024 * 1024;

        private final Context                      context;
        private       boolean                      logEnable;
        private       Logger                       logger;
        private       CacheManager<String, Bitmap> memoryCacheManager;
        private       CacheManager<String, Bitmap> diskCacheManager;
        private       File                         cacheDirectory;
        private       OnLoadListener               listener;
        private       ExecutorService              service;
        private       Bitmap.Config                defaultBitmapConfig;
        private       Downloader                   downloader;
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
