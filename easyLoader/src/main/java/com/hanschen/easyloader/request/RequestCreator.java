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
package com.hanschen.easyloader.request;

import android.app.Notification;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.hanschen.easyloader.BitmapHunter;
import com.hanschen.easyloader.Callback;
import com.hanschen.easyloader.DeferredRequestCreator;
import com.hanschen.easyloader.DiskPolicy;
import com.hanschen.easyloader.Dispatcher;
import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.EnhanceDrawable;
import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.MemoryPolicy;
import com.hanschen.easyloader.Priority;
import com.hanschen.easyloader.Target;
import com.hanschen.easyloader.Transformation;
import com.hanschen.easyloader.action.Action;
import com.hanschen.easyloader.action.FetchAction;
import com.hanschen.easyloader.action.GetAction;
import com.hanschen.easyloader.action.ImageViewAction;
import com.hanschen.easyloader.action.RemoteViewsAction;
import com.hanschen.easyloader.action.TargetAction;
import com.hanschen.easyloader.log.EasyLoaderLog;
import com.hanschen.easyloader.util.ThreadChecker;
import com.hanschen.easyloader.util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hanschen.easyloader.MemoryPolicy.shouldReadFromMemoryCache;

public class RequestCreator {

    private static final AtomicInteger nextId = new AtomicInteger();

    private final EasyLoader         loader;
    private final RequestTransformer requestTransformer;
    private final Dispatcher         dispatcher;
    private final Request.Builder    builder;

    private boolean noFade;
    private boolean deferred;
    private boolean setPlaceholder = true;
    private int      placeholderResId;
    private int      errorResId;
    private int      memoryPolicy;
    private int      diskPolicy;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private Object   tag;

    public RequestCreator(EasyLoader loader,
                          Uri uri,
                          int resourceId,
                          RequestTransformer requestTransformer,
                          Dispatcher dispatcher) {
        if (loader.isShutdown()) {
            throw new IllegalStateException("EasyLoader instance already shut down. Cannot submit new requests.");
        }
        if ((uri != null) == (resourceId != 0)) {
            throw new IllegalArgumentException("uri and resourceId can not both be null or not null");
        }
        this.requestTransformer = requestTransformer;
        this.loader = loader;
        this.dispatcher = dispatcher;
        this.builder = new Request.Builder(uri, resourceId, loader.getDefaultBitmapConfig());
    }

    public RequestCreator noPlaceholder() {
        if (placeholderResId != 0) {
            throw new IllegalStateException("Placeholder resource already set.");
        }
        if (placeholderDrawable != null) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        setPlaceholder = false;
        return this;
    }

    public RequestCreator placeholder(int placeholderResId) {
        if (!setPlaceholder) {
            throw new IllegalStateException("Already explicitly declared as no placeholder.");
        }
        if (placeholderResId == 0) {
            throw new IllegalArgumentException("Placeholder image resource invalid.");
        }
        if (placeholderDrawable != null) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.placeholderResId = placeholderResId;
        return this;
    }

    public RequestCreator placeholder(Drawable placeholderDrawable) {
        if (!setPlaceholder) {
            throw new IllegalStateException("Already explicitly declared as no placeholder.");
        }
        if (placeholderResId != 0) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.placeholderDrawable = placeholderDrawable;
        return this;
    }

    public RequestCreator error(int errorResId) {
        if (errorResId == 0) {
            throw new IllegalArgumentException("Error image resource invalid.");
        }
        if (errorDrawable != null) {
            throw new IllegalStateException("Error image already set.");
        }
        this.errorResId = errorResId;
        return this;
    }

    public RequestCreator error(Drawable errorDrawable) {
        if (errorDrawable == null) {
            throw new IllegalArgumentException("Error image may not be null.");
        }
        if (errorResId != 0) {
            throw new IllegalStateException("Error image already set.");
        }
        this.errorDrawable = errorDrawable;
        return this;
    }

    public RequestCreator tag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag invalid.");
        }
        if (this.tag != null) {
            throw new IllegalStateException("Tag already set.");
        }
        this.tag = tag;
        return this;
    }

    public RequestCreator clearTag() {
        this.tag = null;
        return this;
    }

    public Object getTag() {
        return tag;
    }

    /**
     * fit is not support now
     */
    public RequestCreator fit() {
        throw new UnsupportedOperationException("fit is not support now");
//        deferred = true;
//        return this;
    }

    public RequestCreator unfit() {
        deferred = false;
        return this;
    }

    public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId) {
        Resources resources = loader.getContext().getResources();
        int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
        int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
        return resize(targetWidth, targetHeight);
    }

    public RequestCreator resize(int targetWidth, int targetHeight) {
        builder.resize(targetWidth, targetHeight);
        return this;
    }

    public RequestCreator centerCrop() {
        builder.centerCrop();
        return this;
    }

    public RequestCreator centerInside() {
        builder.centerInside();
        return this;
    }

    public RequestCreator onlyScaleDown() {
        builder.onlyScaleDown();
        return this;
    }

    public RequestCreator rotate(float degrees) {
        builder.rotate(degrees);
        return this;
    }

    public RequestCreator rotate(float degrees, float pivotX, float pivotY) {
        builder.rotate(degrees, pivotX, pivotY);
        return this;
    }

    public RequestCreator config(Bitmap.Config config) {
        builder.config(config);
        return this;
    }

    public RequestCreator stableKey(String stableKey) {
        builder.stableKey(stableKey);
        return this;
    }

    public RequestCreator priority(Priority priority) {
        builder.priority(priority);
        return this;
    }

    public RequestCreator transform(Transformation transformation) {
        builder.transform(transformation);
        return this;
    }

    public RequestCreator transform(List<? extends Transformation> transformations) {
        builder.transform(transformations);
        return this;
    }

    public RequestCreator memoryPolicy(MemoryPolicy policy, MemoryPolicy... additional) {
        if (policy == null) {
            throw new IllegalArgumentException("Memory policy cannot be null.");
        }
        this.memoryPolicy |= policy.index;
        if (additional == null) {
            throw new IllegalArgumentException("Memory policy cannot be null.");
        }
        for (MemoryPolicy memoryPolicy : additional) {
            if (memoryPolicy == null) {
                throw new IllegalArgumentException("Memory policy cannot be null.");
            }
            this.memoryPolicy |= memoryPolicy.index;
        }
        return this;
    }

    public RequestCreator diskPolicy(DiskPolicy policy, DiskPolicy... additional) {
        if (policy == null) {
            throw new IllegalArgumentException("Network policy cannot be null.");
        }
        this.diskPolicy |= policy.index;
        if (additional == null) {
            throw new IllegalArgumentException("Network policy cannot be null.");
        }
        for (DiskPolicy diskPolicy : additional) {
            if (diskPolicy == null) {
                throw new IllegalArgumentException("Network policy cannot be null.");
            }
            this.diskPolicy |= diskPolicy.index;
        }
        return this;
    }

    public RequestCreator purgeable() {
        builder.purgeable();
        return this;
    }

    public RequestCreator noFade() {
        noFade = true;
        return this;
    }

    /**
     * 创建请求并同步等待返回结果，注意请不要在主线程中调用
     *
     * @return
     * @throws IOException
     */
    public Bitmap get() throws IOException {
        ThreadChecker.checkNotMain();
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with get.");
        }
        if (!builder.hasImage()) {
            return null;
        }

        long started = System.nanoTime();
        Request finalRequest = createRequest(started);
        String key = Utils.createKey(finalRequest, new StringBuilder());

        Action action = new GetAction(loader, finalRequest, memoryPolicy, diskPolicy, tag, key);
        return BitmapHunter.forRequest(loader, dispatcher, loader.getMemoryCacheManager(), loader.getDiskCacheManager(), action)
                           .hunt();
    }

    /**
     * 异步请求图片，可用作事先对图片进行缓存，可在任意线程调用
     */
    public void fetch() {
        fetch(null);
    }

    /**
     * 异步请求图片，可用作事先对图片进行缓存，可在任意线程调用,callback会被强引用，从而可能导致callback相关的Activity或Fragment在请求完成之前不能被释放
     */
    public void fetch(Callback callback) {

        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with fetch.");
        }
        if (builder.hasImage()) {
            if (!builder.hasPriority()) {
                builder.priority(Priority.LOW);
            }

            long started = System.nanoTime();
            Request finalRequest = createRequest(started);
            String key = Utils.createKey(finalRequest, new StringBuilder());

            Action action = new FetchAction(loader, finalRequest, memoryPolicy, diskPolicy, tag, key, callback);
            loader.submit(action);
        }
    }

    /**
     * 异步获取图片，一般而言，{@link Target}应该是一个{@link android.view.View View}，该方法持有target的弱引用
     */
    public void into(Target target) {

        ThreadChecker.checkMain();
        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with a Target.");
        }

        if (!builder.hasImage()) {
            loader.cancelRequest(target);
            target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);
            return;
        }

        long started = System.nanoTime();
        Request finalRequest = createRequest(started);
        String key = Utils.createKey(finalRequest);

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            Bitmap bitmap = loader.quickMemoryCacheCheck(key);
            if (bitmap != null) {
                loader.cancelRequest(target);
                target.onBitmapLoaded(bitmap, LoadedFrom.MEMORY);
                return;
            }
        }

        target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);

        Action action = new TargetAction(loader,
                                         target,
                                         finalRequest,
                                         memoryPolicy,
                                         diskPolicy,
                                         errorDrawable,
                                         key,
                                         tag,
                                         errorResId);
        loader.enqueueAndSubmit(action);
    }

    public void into(RemoteViews remoteViews, int viewId, int notificationId, Notification notification) {
        into(remoteViews, viewId, notificationId, notification, null);
    }

    /**
     * 为{@link RemoteViews}异步加载图片，可用于Notification
     */
    public void into(RemoteViews remoteViews, int viewId, int notificationId, Notification notification, String notificationTag) {

        if (remoteViews == null) {
            throw new IllegalArgumentException("RemoteViews must not be null.");
        }
        if (notification == null) {
            throw new IllegalArgumentException("Notification must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with RemoteViews.");
        }
        if (placeholderDrawable != null || placeholderResId != 0 || errorDrawable != null) {
            throw new IllegalArgumentException("Cannot use placeholder or onError drawables with remote views.");
        }

        long started = System.nanoTime();
        Request request = createRequest(started);
        String key = Utils.createKey(request, new StringBuilder());

        RemoteViewsAction action = new RemoteViewsAction.NotificationAction(loader,
                                                                            request,
                                                                            remoteViews,
                                                                            viewId,
                                                                            notificationId,
                                                                            notification,
                                                                            notificationTag,
                                                                            memoryPolicy,
                                                                            diskPolicy,
                                                                            key,
                                                                            tag,
                                                                            errorResId);

        performRemoteViewInto(action);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into all instances of a widget.
     */
    public void into(RemoteViews remoteViews, int viewId, int[] appWidgetIds) {
        long started = System.nanoTime();

        if (remoteViews == null) {
            throw new IllegalArgumentException("remoteViews must not be null.");
        }
        if (appWidgetIds == null) {
            throw new IllegalArgumentException("appWidgetIds must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with remote views.");
        }
        if (placeholderDrawable != null || placeholderResId != 0 || errorDrawable != null) {
            throw new IllegalArgumentException("Cannot use placeholder or onError drawables with remote views.");
        }

        Request request = createRequest(started);
        String key = Utils.createKey(request, new StringBuilder()); // Non-main thread needs own builder.

        RemoteViewsAction action = new RemoteViewsAction.AppWidgetAction(loader,
                                                                         request,
                                                                         remoteViews,
                                                                         viewId,
                                                                         appWidgetIds,
                                                                         memoryPolicy,
                                                                         diskPolicy,
                                                                         key,
                                                                         tag,
                                                                         errorResId);

        performRemoteViewInto(action);
    }

    /**
     * 为{@link ImageView}异步加载图片，持有ImageView的弱引用
     */
    public void into(ImageView target) {
        into(target, null);
    }

    public void into(ImageView target, Callback callback) {

        ThreadChecker.checkMain();

        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }

        if (!builder.hasImage()) {
            loader.cancelRequest(target);
            if (setPlaceholder) {
                EnhanceDrawable.setPlaceholder(target, getPlaceholderDrawable());
            }
            return;
        }

        if (deferred) {
            if (builder.hasSize()) {
                throw new IllegalStateException("Fit cannot be used with resize.");
            }
            int width = target.getWidth();
            int height = target.getHeight();
            if (width == 0 || height == 0) {
                if (setPlaceholder) {
                    EnhanceDrawable.setPlaceholder(target, getPlaceholderDrawable());
                }
                loader.defer(target, new DeferredRequestCreator(this, target, callback));
                return;
            }
            builder.resize(width, height);
        }

        long started = System.nanoTime();
        Request request = createRequest(started);
        EasyLoaderLog.d("request: ", request.toString());
        String key = Utils.createKey(request);

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            Bitmap bitmap = loader.quickMemoryCacheCheck(key);
            if (bitmap != null) {
                loader.cancelRequest(target);
                EnhanceDrawable.setBitmap(target,
                                          loader.getContext(),
                                          bitmap,
                                          LoadedFrom.MEMORY,
                                          noFade,
                                          loader.isIndicatorsEnabled());
                if (callback != null) {
                    callback.onSuccess();
                }
                return;
            }
        }

        if (setPlaceholder) {
            EnhanceDrawable.setPlaceholder(target, getPlaceholderDrawable());
        }

        Action action = new ImageViewAction(loader,
                                            target,
                                            request,
                                            memoryPolicy,
                                            diskPolicy,
                                            errorResId,
                                            errorDrawable,
                                            key,
                                            tag,
                                            callback,
                                            noFade);

        loader.enqueueAndSubmit(action);
    }

    private Drawable getPlaceholderDrawable() {
        if (placeholderResId != 0) {
            return loader.getContext().getResources().getDrawable(placeholderResId);
        } else {
            return placeholderDrawable;
        }
    }

    private Request transformRequest(Request request) {
        if (requestTransformer != null) {
            Request transformed = requestTransformer.transformRequest(request);
            if (transformed == null) {
                throw new IllegalStateException("Request transformer " + requestTransformer.getClass()
                                                                                           .getCanonicalName() + " returned null for " + request);
            }
            return transformed;
        }
        return request;
    }

    private Request createRequest(long started) {

        Request request = transformRequest(builder.build());
        request.id = nextId.getAndIncrement();
        request.started = started;

        return request;
    }

    private void performRemoteViewInto(RemoteViewsAction action) {
        if (shouldReadFromMemoryCache(memoryPolicy)) {
            Bitmap bitmap = loader.quickMemoryCacheCheck(action.getKey());
            if (bitmap != null) {
                action.onComplete(bitmap, LoadedFrom.MEMORY);
                return;
            }
        }

        if (placeholderResId != 0) {
            action.setImageResource(placeholderResId);
        }

        loader.enqueueAndSubmit(action);
    }
}
