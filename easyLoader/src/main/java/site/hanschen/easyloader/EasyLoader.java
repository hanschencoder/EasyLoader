/*
 * Copyright 2016 Hans Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package site.hanschen.easyloader;

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

import site.hanschen.easyloader.action.Action;
import site.hanschen.easyloader.cache.CacheManager;
import site.hanschen.easyloader.cache.LruDiskCache;
import site.hanschen.easyloader.cache.LruMemoryCache;
import site.hanschen.easyloader.cache.SizeCalculator;
import site.hanschen.easyloader.callback.OnLoadListener;
import site.hanschen.easyloader.downloader.Downloader;
import site.hanschen.easyloader.downloader.OkHttp3Downloader;
import site.hanschen.easyloader.log.EasyLoaderLog;
import site.hanschen.easyloader.request.AssetRequestHandler;
import site.hanschen.easyloader.request.ContactsPhotoRequestHandler;
import site.hanschen.easyloader.request.ContentStreamRequestHandler;
import site.hanschen.easyloader.request.FileRequestHandler;
import site.hanschen.easyloader.request.MediaStoreRequestHandler;
import site.hanschen.easyloader.request.NetworkRequestHandler;
import site.hanschen.easyloader.request.RequestCreator;
import site.hanschen.easyloader.request.RequestHandler;
import site.hanschen.easyloader.request.RequestTransformer;
import site.hanschen.easyloader.request.ResourceRequestHandler;
import site.hanschen.easyloader.util.AppUtils;
import site.hanschen.easyloader.util.BitmapUtils;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static site.hanschen.easyloader.MemoryPolicy.shouldReadFromMemoryCache;
import static site.hanschen.easyloader.util.ThreadChecker.checkMain;

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
    private final QueueProcessType                       queueProcessType;
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
                       QueueProcessType queueProcessType,
                       boolean indicatorsEnabled,
                       boolean loggingEnabled) {

        EasyLoaderLog.logEnable(loggingEnabled);
        this.context = context.getApplicationContext();
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
        this.listener = listener;
        this.defaultBitmapConfig = defaultBitmapConfig;
        this.requestTransformer = requestTransformer;
        this.queueProcessType = queueProcessType;
        this.indicatorsEnabled = indicatorsEnabled;

        //初始化requestHandlers
        List<RequestHandler> allRequestHandlers = new ArrayList<>();
        allRequestHandlers.add(new ResourceRequestHandler(context));
        allRequestHandlers.add(new ContactsPhotoRequestHandler(context));
        allRequestHandlers.add(new MediaStoreRequestHandler(context));
        allRequestHandlers.add(new ContentStreamRequestHandler(context));
        allRequestHandlers.add(new AssetRequestHandler(context));
        allRequestHandlers.add(new FileRequestHandler(context));
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
                        hunter.getLoader().complete(hunter);
                    }
                    break;
                }
                case Dispatcher.REQUEST_GCED: {
                    Action action = (Action) msg.obj;
                    action.getLoader().cancelExistingRequest(action.getTarget());
                    break;
                }
                case Dispatcher.REQUEST_BATCH_RESUME:
                    @SuppressWarnings("unchecked") List<Action> batch = (List<Action>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, n = batch.size(); i < n; i++) {
                        Action action = batch.get(i);
                        action.getLoader().resumeAction(action);
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

    /**
     * 处理成功o或失败的请求
     *
     * @param hunter 待处理的请求
     */
    private void complete(BitmapHunter hunter) {

        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();

        boolean hasMultiple = joined != null && !joined.isEmpty();

        if (single == null && !hasMultiple) {
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

    /**
     * hunter结束后，处理action
     *
     * @param result 结果
     * @param from   加载方式
     * @param action action
     */
    private void deliverAction(Bitmap result, LoadedFrom from, Action action) {
        //再次检测下action是否被cancel了
        if (action.isCancelled()) {
            return;
        }
        //willReplay默认为否，除非请求失败了，加入了联网重试列表
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
                    singleton = new Builder(context).downloader(new OkHttp3Downloader())
                                                    .indicatorsEnabled(true)
                                                    .logEnable(true)
                                                    .maxMemoryCacheSize(60 * 1024 * 1024)// TODO: 2016/10/26 根据设备型号计算
                                                    .maxDiskCacheSize(100 * 1024 * 1024)// TODO: 2016/10/26 根据存储状态自动计算
                                                    .queueProcessType(QueueProcessType.LIFO)
                                                    .build();
                }
            }
        }
        return singleton;
    }

    /**
     * 加载uri指定图片
     */
    public RequestCreator load(Uri uri) {
        return new RequestCreator(EasyLoader.this, uri, 0, requestTransformer, dispatcher);
    }

    /**
     * 加载资源ID对应的图片
     */
    public RequestCreator load(int resourceId) {
        if (resourceId == 0) {
            throw new IllegalArgumentException("Resource ID must not be zero.");
        }
        return new RequestCreator(EasyLoader.this, null, resourceId, requestTransformer, dispatcher);
    }

    /**
     * 加载文件
     *
     * @param file 若传入{@code null}将不会发起任何请求，但会设置placeholder
     */
    public RequestCreator load(File file) {
        if (file == null) {
            return new RequestCreator(this, null, 0, requestTransformer, dispatcher);
        }
        return load(Uri.fromFile(file));
    }

    /**
     * 加载uri对应的图片
     */
    public RequestCreator load(String uri) {
        if (uri == null) {
            return new RequestCreator(this, null, 0, requestTransformer, dispatcher);
        }
        if (uri.trim().length() == 0) {
            throw new IllegalArgumentException("Path must not be empty.");
        }
        return load(Uri.parse(uri));
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
    public QueueProcessType getQueueProcessType() {
        return queueProcessType;
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

        private static final long DEFAULT_DISK_CACHE_SIZE   = 250 * 1024 * 1024;
        private static final long DEFAULT_MEMORY_CACHE_SIZE = 60 * 1024 * 1024;

        private final Context                      context;
        private       AdjustableExecutorService    service;
        private       boolean                      logEnable;
        private       boolean                      indicatorsEnabled;
        private       CacheManager<String, Bitmap> memoryCacheManager;
        private       CacheManager<String, Bitmap> diskCacheManager;
        private       File                         cacheDirectory;
        private       OnLoadListener               listener;
        private       RequestTransformer           transformer;
        private       Bitmap.Config                defaultBitmapConfig;
        private       Downloader                   downloader;
        private       QueueProcessType             queueProcessType;
        private       List<RequestHandler>         requestHandlers;
        private long maxMemoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE;
        private long maxDiskCacheSize   = DEFAULT_DISK_CACHE_SIZE;

        private Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context can not be null");
            }
            this.context = context.getApplicationContext();
        }

        Builder logEnable(boolean enable) {
            this.logEnable = enable;
            return this;
        }

        Builder indicatorsEnabled(boolean enable) {
            this.indicatorsEnabled = enable;
            return this;
        }

        Builder memoryCache(CacheManager<String, Bitmap> memoryCache) {
            this.memoryCacheManager = memoryCache;
            return this;
        }

        Builder diskCache(CacheManager<String, Bitmap> diskCache) {
            this.diskCacheManager = diskCache;
            return this;
        }

        Builder cacheDirectory(File cacheDir) {
            this.cacheDirectory = cacheDir;
            return this;
        }

        Builder onLoadListener(OnLoadListener onLoadListener) {
            this.listener = onLoadListener;
            return this;
        }

        Builder RequestTransformer(RequestTransformer transformer) {
            this.transformer = transformer;
            return this;
        }

        Builder defaultBitmapConfig(Bitmap.Config config) {
            this.defaultBitmapConfig = config;
            return this;
        }

        Builder downloader(Downloader downloader) {
            this.downloader = downloader;
            return this;
        }

        Builder queueProcessType(QueueProcessType queueProcessType) {
            this.queueProcessType = queueProcessType;
            return this;
        }

        public Builder requestHandlers(List<RequestHandler> handlers) {
            this.requestHandlers = handlers;
            return this;
        }

        Builder maxMemoryCacheSize(long maxMemoryCacheSize) {
            this.maxMemoryCacheSize = maxMemoryCacheSize;
            return this;
        }

        Builder maxDiskCacheSize(long maxDiskCacheSize) {
            this.maxDiskCacheSize = maxDiskCacheSize;
            return this;
        }

        EasyLoader build() {

            if (service == null) {
                service = new AdjustableExecutorService();
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
                        diskCacheManager = new LruDiskCache<>(cacheDirectory,
                                                              maxDiskCacheSize,
                                                              AppUtils.getVersionCode(context),
                                                              new LruDiskCache.FileConverter<Bitmap>() {
                                                                  @Override
                                                                  public Bitmap readFrom(File file) {
                                                                      return BitmapFactory.decodeFile(file.getAbsolutePath(),
                                                                                                      null);
                                                                  }

                                                                  @Override
                                                                  public boolean writeTo(Bitmap value, File to) {
                                                                      try {
                                                                          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(
                                                                                  to));
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
                downloader = new OkHttp3Downloader();
            }

            if (queueProcessType == null) {
                queueProcessType = QueueProcessType.LIFO;
            }

            return new EasyLoader(context,
                                  service,
                                  memoryCacheManager,
                                  diskCacheManager,
                                  listener,
                                  requestHandlers,
                                  defaultBitmapConfig,
                                  transformer,
                                  downloader,
                                  queueProcessType,
                                  indicatorsEnabled,
                                  logEnable);
        }
    }
}
