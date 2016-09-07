package com.hanschen.easyloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.cache.LruDiskCache;
import com.hanschen.easyloader.cache.LruMemoryCache;
import com.hanschen.easyloader.cache.SizeCalculator;
import com.hanschen.easyloader.callback.OnLoadListener;
import com.hanschen.easyloader.downloader.Downloader;
import com.hanschen.easyloader.log.Logger;
import com.hanschen.easyloader.request.NetworkRequestHandler;
import com.hanschen.easyloader.request.Request;
import com.hanschen.easyloader.request.RequestCreator;
import com.hanschen.easyloader.request.RequestHandler;
import com.hanschen.easyloader.util.AppUtils;
import com.hanschen.easyloader.util.BitmapUtils;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.hanschen.easyloader.MemoryPolicy.shouldReadFromMemoryCache;
import static com.hanschen.easyloader.util.ThreadChecker.checkMain;

/**
 * Created by Hans.Chen on 2016/7/27.
 */
public class EasyLoader {

    private volatile static EasyLoader                             singleton;
    public                  Context                                context;
    final                   ReferenceQueue<Object>                 referenceQueue;
    volatile                boolean                                loggingEnabled;
    final                   Dispatcher                             dispatcher;
    private final           List<RequestHandler>                   requestHandlers;
    public final            LruMemoryCache<String, Bitmap>         cache;
    public final            Bitmap.Config                          defaultBitmapConfig;
    private final           RequestTransformer                     requestTransformer;
    private final           OnLoadListener                         listener;
    private final           CleanupThread                          cleanupThread;
    public final            Stats                                  stats;
    final                   Map<Object, Action>                    targetToAction;
    final                   Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
    public                  boolean                                shutdown;

    public boolean indicatorsEnabled;

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }


    public interface RequestTransformer {
        /**
         * Transform a request before it is submitted to be processed.
         *
         * @return The original request or a new request to replace it. Must not be null.
         */
        Request transformRequest(Request request);

        /**
         * A {@link RequestTransformer} which returns the original request.
         */
        RequestTransformer IDENTITY = new RequestTransformer() {
            @Override
            public Request transformRequest(Request request) {
                return request;
            }
        };
    }

    private EasyLoader(Context context,
                       Dispatcher dispatcher,
                       LruMemoryCache<String, Bitmap> cache,
                       OnLoadListener listener,
                       List<RequestHandler> extraRequestHandlers,
                       Stats stats,
                       Bitmap.Config defaultBitmapConfig,
                       RequestTransformer requestTransformer,
                       boolean indicatorsEnabled,
                       boolean loggingEnabled) {

        this.context = context;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.listener = listener;
        this.requestTransformer = requestTransformer;
        this.defaultBitmapConfig = defaultBitmapConfig;

        int builtInHandlers = 7; // Adjust this as internal handlers are added or removed.
        int extraCount = (extraRequestHandlers != null ? extraRequestHandlers.size() : 0);
        List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers + extraCount);

        // ResourceRequestHandler needs to be the first in the list to avoid
        // forcing other RequestHandlers to perform null checks on request.uri
        // to cover the (request.resourceId != 0) case.
        if (extraRequestHandlers != null) {
            allRequestHandlers.addAll(extraRequestHandlers);
        }
        allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader));
        requestHandlers = Collections.unmodifiableList(allRequestHandlers);

        this.stats = stats;
        this.targetToAction = new WeakHashMap<>();
        this.targetToDeferredRequestCreator = new WeakHashMap<>();
        this.indicatorsEnabled = indicatorsEnabled;
        this.loggingEnabled = loggingEnabled;
        this.referenceQueue = new ReferenceQueue<>();
        this.cleanupThread = new CleanupThread(referenceQueue, HANDLER);
        this.cleanupThread.start();
    }

    public void shutdown() {
        if (this == singleton) {
            throw new UnsupportedOperationException("Default singleton instance cannot be shutdown.");
        }
        if (shutdown) {
            return;
        }
        cache.clear();
        cleanupThread.shutdown();
        stats.shutdown();
        dispatcher.shutdown();
        for (DeferredRequestCreator deferredRequestCreator : targetToDeferredRequestCreator.values()) {
            deferredRequestCreator.cancel();
        }
        targetToDeferredRequestCreator.clear();
        shutdown = true;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public ReferenceQueue<Object> getReferenceQueue() {
        return referenceQueue;
    }

    public List<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    public Request transformRequest(Request request) {
        Request transformed = requestTransformer.transformRequest(request);
        if (transformed == null) {
            throw new IllegalStateException("Request transformer " + requestTransformer.getClass()
                                                                                       .getCanonicalName() + " returned null for " + request);
        }
        return transformed;
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
                    action.loader.cancelExistingRequest(action.getTarget());
                    break;
                }
                case Dispatcher.REQUEST_BATCH_RESUME:
                    @SuppressWarnings("unchecked") List<Action> batch = (List<Action>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, n = batch.size(); i < n; i++) {
                        Action action = batch.get(i);
                        action.loader.resumeAction(action);
                    }
                    break;
                default:
                    throw new AssertionError("Unknown handler message received: " + msg.what);
            }
        }
    };

    public void cancelRequest(@NonNull ImageView view) {
        // checkMain() is called from cancelExistingRequest()
        if (view == null) {
            throw new IllegalArgumentException("view cannot be null.");
        }
        cancelExistingRequest(view);
    }

    public void cancelRequest(@NonNull Target target) {
        // checkMain() is called from cancelExistingRequest()
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null.");
        }
        cancelExistingRequest(target);
    }

    void complete(BitmapHunter hunter) {
        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();

        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null || hasMultiple;

        if (!shouldDeliver) {
            return;
        }

        Uri uri = hunter.getData().uri;
        Exception exception = hunter.getException();
        Bitmap result = hunter.getResult();
        LoadedFrom from = hunter.getLoadedFrom();

        if (single != null) {
            deliverAction(result, from, single);
        }

        if (hasMultiple) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                deliverAction(result, from, join);
            }
        }

        if (listener != null && exception != null) {
            listener.onLoadFailed(this, uri, exception);
        }
    }

    public void defer(ImageView view, DeferredRequestCreator request) {
        // If there is already a deferred request, cancel it.
        if (targetToDeferredRequestCreator.containsKey(view)) {
            cancelExistingRequest(view);
        }
        targetToDeferredRequestCreator.put(view, request);
    }

    public Bitmap quickMemoryCacheCheck(String key) {
        Bitmap cached = cache.get(key);
        if (cached != null) {
            stats.dispatchCacheHit();
        } else {
            stats.dispatchCacheMiss();
        }
        return cached;
    }


    public void submit(Action action) {
        dispatcher.dispatchSubmit(action);
    }

    public void enqueueAndSubmit(Action action) {
        Object target = action.getTarget();
        if (target != null && targetToAction.get(target) != action) {
            // This will also check we are on the main thread.
            cancelExistingRequest(target);
            targetToAction.put(target, action);
        }
        submit(action);
    }


    void resumeAction(Action action) {
        Bitmap bitmap = null;
        if (shouldReadFromMemoryCache(action.memoryPolicy)) {
            bitmap = quickMemoryCacheCheck(action.getKey());
        }

        if (bitmap != null) {
            // Resumed action is cached, complete immediately.
            deliverAction(bitmap, LoadedFrom.MEMORY, action);
        } else {
            // Re-submit the action to the executor.
            enqueueAndSubmit(action);
        }
    }

    private void deliverAction(Bitmap result, LoadedFrom from, Action action) {
        if (action.isCancelled()) {
            return;
        }
        if (!action.willReplay()) {
            targetToAction.remove(action.getTarget());
        }
        if (result != null) {
            if (from == null) {
                throw new AssertionError("LoadedFrom cannot be null.");
            }
            action.complete(result, from);
        } else {
            action.error();
        }
    }

    private void cancelExistingRequest(Object target) {
        checkMain();
        Action action = targetToAction.remove(target);
        if (action != null) {
            action.cancel();
            dispatcher.dispatchCancel(action);
        }
        if (target instanceof ImageView) {
            ImageView targetImageView = (ImageView) target;
            DeferredRequestCreator deferredRequestCreator = targetToDeferredRequestCreator.remove(targetImageView);
            if (deferredRequestCreator != null) {
                deferredRequestCreator.cancel();
            }
        }
    }

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

    public RequestCreator load(Uri uri) {
        return new RequestCreator(this, uri, 0);
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
        private       RequestTransformer           transformer;
        private       ExecutorService              service;
        private       Bitmap.Config                defaultBitmapConfig;
        private       Downloader                   downloader;
        private       List<RequestHandler>         requestHandlers;
        private       boolean                      indicatorsEnabled;
        private       boolean                      loggingEnabled;
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


            EasyLoader loader = new EasyLoader(null, null, null, null, null, null, null, null, true, true);
            loader.apply(Builder.this);
            return loader;
        }
    }
}
