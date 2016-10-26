package com.hanschen.easyloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;

import com.hanschen.easyloader.action.Action;
import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.cache.LruDiskCache;
import com.hanschen.easyloader.cache.LruMemoryCache;
import com.hanschen.easyloader.cache.SizeCalculator;
import com.hanschen.easyloader.callback.OnLoadListener;
import com.hanschen.easyloader.downloader.Downloader;
import com.hanschen.easyloader.downloader.Okhttp3Downloader;
import com.hanschen.easyloader.log.EasyLoaderLog;
import com.hanschen.easyloader.log.Logger;
import com.hanschen.easyloader.request.NetworkRequestHandler;
import com.hanschen.easyloader.request.RequestCreator;
import com.hanschen.easyloader.request.RequestHandler;
import com.hanschen.easyloader.request.RequestTransformer;
import com.hanschen.easyloader.util.AppUtils;
import com.hanschen.easyloader.util.BitmapUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.hanschen.easyloader.MemoryPolicy.shouldReadFromMemoryCache;
import static com.hanschen.easyloader.util.ThreadChecker.checkMain;

/**
 * Created by Hans.Chen on 2016/7/27.
 */
public class EasyLoader implements Provider {

    @SuppressLint("StaticFieldLeak")
    private volatile static EasyLoader singleton;

    private final Context                                context;
    private final ReferenceQueue<Object>                 referenceQueue;
    private final Dispatcher                             dispatcher;
    private final List<RequestHandler>                   requestHandlers;
    private final CacheManager<String, Bitmap>           memoryCache;
    private final CacheManager<String, Bitmap>           diskCache;
    private final Bitmap.Config                          defaultBitmapConfig;
    private final RequestTransformer                     requestTransformer;
    private final OnLoadListener                         listener;
    private final CleanupThread                          cleanupThread;
    private final Map<Object, Action>                    targetToAction;
    private final Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
    private       boolean                                shutdown;
    private       boolean                                indicatorsEnabled;

    private EasyLoader(Context context,
                       AdjustableExecutorService service,
                       CacheManager<String, Bitmap> memoryCache,
                       CacheManager<String, Bitmap> diskCache,
                       OnLoadListener listener,
                       List<RequestHandler> extraRequestHandlers,
                       Bitmap.Config defaultBitmapConfig,
                       RequestTransformer requestTransformer,
                       Downloader downloader,
                       boolean indicatorsEnabled,
                       boolean loggingEnabled) {

        EasyLoaderLog.logEnable(loggingEnabled);
        this.context = context.getApplicationContext();
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
        this.listener = listener;
        this.defaultBitmapConfig = defaultBitmapConfig;
        this.requestTransformer = requestTransformer;
        this.indicatorsEnabled = indicatorsEnabled;

        //初始化requestHandlers
        List<RequestHandler> allRequestHandlers = new ArrayList<>();
        allRequestHandlers.add(new NetworkRequestHandler(downloader));
        if (extraRequestHandlers != null) {
            allRequestHandlers.addAll(extraRequestHandlers);
        }
        requestHandlers = Collections.unmodifiableList(allRequestHandlers);

        this.dispatcher = new Dispatcher(context, service, HANDLER, memoryCache, diskCache);
        this.targetToAction = new WeakHashMap<>();
        this.targetToDeferredRequestCreator = new WeakHashMap<>();
        this.referenceQueue = new ReferenceQueue<>();
        this.cleanupThread = new CleanupThread(referenceQueue, HANDLER);
        this.cleanupThread.start();
    }

    @VisibleForTesting
    public void shutdown() {
        if (shutdown) {
            return;
        }
        memoryCache.clear();
        cleanupThread.shutdown();
        dispatcher.shutdown();
        for (DeferredRequestCreator deferredRequestCreator : targetToDeferredRequestCreator.values()) {
            deferredRequestCreator.cancel();
        }
        targetToDeferredRequestCreator.clear();
        shutdown = true;
    }

    static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Dispatcher.HUNTER_BATCH_COMPLETE: {
                    @SuppressWarnings("unchecked") List<BitmapHunter> batch = (List<BitmapHunter>) msg.obj;
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
                    throw new AssertionError("Unknown dispatcherHandler message received: " + msg.what);
            }
        }
    };

    /**
     * 取消target对应的请求
     *
     * @param target target
     */
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

    /**
     * 提交Action
     *
     * @param action 提交Action
     */
    public void submit(Action action) {
        dispatcher.dispatchSubmit(action);
    }

    /**
     * 保存Target-Action对应关系，提交Action
     */
    public void enqueueAndSubmit(Action action) {
        Object target = action.getTarget();
        if (target != null && targetToAction.get(target) != action) {
            cancelExistingRequest(target);
            targetToAction.put(target, action);
        }
        submit(action);
    }

    public <T> void cancelRequest(T target) {
        if (target == null) {
            throw new IllegalArgumentException("view cannot be null.");
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
        return memoryCache.get(key);
    }


    void resumeAction(Action action) {
        Bitmap bitmap = null;
        if (shouldReadFromMemoryCache(action.getMemoryPolicy())) {
            bitmap = quickMemoryCacheCheck(action.getKey());
        }

        if (bitmap != null) {
            // Resumed action is cached, onComplete immediately.
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
            action.onComplete(result, from);
        } else {
            action.onError();
        }
    }

    /**
     * 当Action中的target被GC掉之后，会把target的弱引用保存到{@link EasyLoader#referenceQueue}中
     * 通过检查检查这个队列，把target对应的Action取消
     */
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
                    // FIXME: 2016/10/26 referenceQueue队列的弱引用通过get方法应该一直返回null，如何得到对应的target？
                    Action.RequestWeakReference<?> remove = (Action.RequestWeakReference<?>) referenceQueue.remove(1000);
                    if (remove != null) {
                        Message message = handler.obtainMessage();
                        message.what = Dispatcher.REQUEST_GCED;
                        message.obj = remove.getAction();
                        handler.sendMessage(message);
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

    public RequestCreator load(Uri uri) {
        return new RequestCreator(EasyLoader.this, uri, 0, requestTransformer, dispatcher);
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public ReferenceQueue<Object> getReferenceQueue() {
        return referenceQueue;
    }

    @Override
    public List<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    @Override
    public CacheManager<String, Bitmap> getMemoryCacheManager() {
        return memoryCache;
    }

    @Override
    public CacheManager<String, Bitmap> getDiskCacheManager() {
        return diskCache;
    }

    @Override
    public Bitmap.Config getDefaultBitmapConfig() {
        return defaultBitmapConfig;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isIndicatorsEnabled() {
        return indicatorsEnabled;
    }

    private static class Builder {

        private static final long DEFAULT_DISK_CACHE_SIZE   = 100 * 1024 * 1024;
        private static final long DEFAULT_MEMORY_CACHE_SIZE = 250 * 1024 * 1024;

        private final Context                      context;
        private       AdjustableExecutorService    service;
        private       boolean                      logEnable;
        private       Logger                       logger;
        private       CacheManager<String, Bitmap> memoryCacheManager;
        private       CacheManager<String, Bitmap> diskCacheManager;
        private       File                         cacheDirectory;
        private       OnLoadListener               listener;
        private       RequestTransformer           transformer;
        private       Bitmap.Config                defaultBitmapConfig;
        private       Downloader                   downloader;
        private       List<RequestHandler>         requestHandlers;
        private       boolean                      indicatorsEnabled;
        private       boolean                      loggingEnabled;
        private long maxMemoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE;
        private long maxDiskCacheSize   = DEFAULT_DISK_CACHE_SIZE;

        private Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context can not be null");
            }
            this.context = context.getApplicationContext();
        }

        public EasyLoader build() {

            if (service == null) {
                service = new AdjustableExecutorService(context);
            }

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
                            public Bitmap readFrom(File file) {
                                return BitmapFactory.decodeFile(file.getAbsolutePath(), null);
                            }

                            @Override
                            public boolean writeTo(Bitmap value, File to) {
                                try {
                                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(to));
                                    value.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                                    bos.flush();
                                    bos.close();
                                    return true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return false;
                            }
                        });
                    }

                }
            }

            if (transformer == null) {
                transformer = RequestTransformer.IDENTITY;
            }

            if (downloader == null) {
                downloader = new Okhttp3Downloader();
            }


            EasyLoader loader = new EasyLoader(context, service, memoryCacheManager, diskCacheManager, null, null, null, transformer, downloader, true, true);
            loader.apply(Builder.this);
            return loader;
        }
    }
}
