package com.hanschen.easyloader.request;

import android.app.Notification;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.hanschen.easyloader.Action;
import com.hanschen.easyloader.BitmapHunter;
import com.hanschen.easyloader.Callback;
import com.hanschen.easyloader.DeferredRequestCreator;
import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.FetchAction;
import com.hanschen.easyloader.GetAction;
import com.hanschen.easyloader.ImageViewAction;
import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.MemoryPolicy;
import com.hanschen.easyloader.NetworkPolicy;
import com.hanschen.easyloader.PicassoDrawable;
import com.hanschen.easyloader.Priority;
import com.hanschen.easyloader.RemoteViewsAction;
import com.hanschen.easyloader.Target;
import com.hanschen.easyloader.TargetAction;
import com.hanschen.easyloader.Transformation;
import com.hanschen.easyloader.util.ThreadChecker;
import com.hanschen.easyloader.util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hanschen.easyloader.MemoryPolicy.shouldReadFromMemoryCache;
import static com.hanschen.easyloader.util.Utils.log;

/**
 * Created by Hans.Chen on 2016/8/19.
 */
public class RequestCreator {

    private static final AtomicInteger nextId = new AtomicInteger();

    private final EasyLoader      loader;
    private final Request.Builder data;

    private boolean noFade;
    private boolean deferred;
    private boolean setPlaceholder = true;
    private int      placeholderResId;
    private int      errorResId;
    private int      memoryPolicy;
    private int      networkPolicy;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private Object   tag;

    RequestCreator(EasyLoader loader, Uri uri, int resourceId) {
//        if (picasso.shutdown) {
//            throw new IllegalStateException("Picasso instance already shut down. Cannot submit new requests.");
//        }
        this.loader = loader;
        this.data = new Request.Builder(uri, resourceId, loader.getBitmapConfig());
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

    public RequestCreator fit() {
        deferred = true;
        return this;
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
        data.resize(targetWidth, targetHeight);
        return this;
    }

    public RequestCreator centerCrop() {
        data.centerCrop();
        return this;
    }

    public RequestCreator centerInside() {
        data.centerInside();
        return this;
    }

    public RequestCreator onlyScaleDown() {
        data.onlyScaleDown();
        return this;
    }

    public RequestCreator rotate(float degrees) {
        data.rotate(degrees);
        return this;
    }

    public RequestCreator rotate(float degrees, float pivotX, float pivotY) {
        data.rotate(degrees, pivotX, pivotY);
        return this;
    }

    public RequestCreator config(Bitmap.Config config) {
        data.config(config);
        return this;
    }

    public RequestCreator stableKey(String stableKey) {
        data.stableKey(stableKey);
        return this;
    }

    public RequestCreator priority(Priority priority) {
        data.priority(priority);
        return this;
    }

    public RequestCreator transform(Transformation transformation) {
        data.transform(transformation);
        return this;
    }

    public RequestCreator transform(List<? extends Transformation> transformations) {
        data.transform(transformations);
        return this;
    }

    @Deprecated
    public RequestCreator skipMemoryCache() {
        return memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE);
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

    public RequestCreator networkPolicy(NetworkPolicy policy, NetworkPolicy... additional) {
        if (policy == null) {
            throw new IllegalArgumentException("Network policy cannot be null.");
        }
        this.networkPolicy |= policy.index;
        if (additional == null) {
            throw new IllegalArgumentException("Network policy cannot be null.");
        }
        for (NetworkPolicy networkPolicy : additional) {
            if (networkPolicy == null) {
                throw new IllegalArgumentException("Network policy cannot be null.");
            }
            this.networkPolicy |= networkPolicy.index;
        }
        return this;
    }

    public RequestCreator purgeable() {
        data.purgeable();
        return this;
    }

    public RequestCreator noFade() {
        noFade = true;
        return this;
    }

    public Bitmap get() throws IOException {
        long started = System.nanoTime();
        ThreadChecker.checkNotMain();

        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with get.");
        }
        if (!data.hasImage()) {
            return null;
        }

        Request finalData = createRequest(started);
        String key = Utils.createKey(finalData, new StringBuilder());

        Action action = new GetAction(loader, finalData, memoryPolicy, networkPolicy, tag, key);
        return BitmapHunter.forRequest(loader, loader.getDispatcher(), loader.cache, loader.stats, action).hunt();
    }

    /**
     * Asynchronously fulfills the request without a {@link ImageView} or {@link Target}. This is
     * useful when you want to warm up the cache with an image.
     * <p/>
     * <em>Note:</em> It is safe to invoke this method from any thread.
     */
    public void fetch() {
        fetch(null);
    }

    /**
     * Asynchronously fulfills the request without a {@link ImageView} or {@link Target},
     * and invokes the target {@link Callback} with the result. This is useful when you want to warm
     * up the cache with an image.
     * <p/>
     * <em>Note:</em> The {@link Callback} param is a strong reference and will prevent your
     * {@link android.app.Activity} or {@link android.app.Fragment} from being garbage collected
     * until the request is completed.
     */
    public void fetch(Callback callback) {
        long started = System.nanoTime();

        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with fetch.");
        }
        if (data.hasImage()) {
            // Fetch requests have lower priority by default.
            if (!data.hasPriority()) {
                data.priority(Priority.LOW);
            }

            Request request = createRequest(started);
            String key = Utils.createKey(request, new StringBuilder());

            if (shouldReadFromMemoryCache(memoryPolicy)) {
                Bitmap bitmap = loader.quickMemoryCacheCheck(key);
                if (bitmap != null) {
                    if (loader.isLoggingEnabled()) {
                        log(Utils.OWNER_MAIN, Utils.VERB_COMPLETED, request.plainId(), "from " + LoadedFrom.MEMORY);
                    }
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                }
            }

            Action action = new FetchAction(loader, request, memoryPolicy, networkPolicy, tag, key, callback);
            loader.submit(action);
        }
    }

    /**
     * Asynchronously fulfills the request into the specified {@link Target}. In most cases, you
     * should use this when you are dealing with a custom {@link android.view.View View} or view
     * holder which should implement the {@link Target} interface.
     * <p/>
     * Implementing on a {@link android.view.View View}:
     * <blockquote><pre>
     * public class ProfileView extends FrameLayout implements Target {
     *   {@literal @}Override public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
     *     setBackgroundDrawable(new BitmapDrawable(bitmap));
     *   }
     * <p/>
     *   {@literal @}Override public void onBitmapFailed() {
     *     setBackgroundResource(R.drawable.profile_error);
     *   }
     * <p/>
     *   {@literal @}Override public void onPrepareLoad(Drawable placeHolderDrawable) {
     *     frame.setBackgroundDrawable(placeHolderDrawable);
     *   }
     * }
     * </pre></blockquote>
     * Implementing on a view holder object for use inside of an adapter:
     * <blockquote><pre>
     * public class ViewHolder implements Target {
     *   public FrameLayout frame;
     *   public TextView name;
     * <p/>
     *   {@literal @}Override public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
     *     frame.setBackgroundDrawable(new BitmapDrawable(bitmap));
     *   }
     * <p/>
     *   {@literal @}Override public void onBitmapFailed() {
     *     frame.setBackgroundResource(R.drawable.profile_error);
     *   }
     * <p/>
     *   {@literal @}Override public void onPrepareLoad(Drawable placeHolderDrawable) {
     *     frame.setBackgroundDrawable(placeHolderDrawable);
     *   }
     * }
     * </pre></blockquote>
     * <p/>
     * <em>Note:</em> This method keeps a weak reference to the {@link Target} instance and will be
     * garbage collected if you do not keep a strong reference to it. To receive callbacks when an
     * image is loaded use {@link #into(android.widget.ImageView, Callback)}.
     */
    public void into(Target target) {
        long started = System.nanoTime();
        ThreadChecker.checkMain();

        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with a Target.");
        }

        if (!data.hasImage()) {
            loader.cancelRequest(target);
            target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);
            return;
        }

        Request request = createRequest(started);
        String requestKey = Utils.createKey(request);

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            Bitmap bitmap = loader.quickMemoryCacheCheck(requestKey);
            if (bitmap != null) {
                loader.cancelRequest(target);
                target.onBitmapLoaded(bitmap, LoadedFrom.MEMORY);
                return;
            }
        }

        target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);

        Action action = new TargetAction(loader, target, request, memoryPolicy, networkPolicy, errorDrawable, requestKey, tag, errorResId);
        loader.enqueueAndSubmit(action);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into a {@link Notification}.
     */
    public void into(RemoteViews remoteViews, int viewId, int notificationId, Notification notification) {
        into(remoteViews, viewId, notificationId, notification, null);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into a {@link Notification}.
     */
    public void into(RemoteViews remoteViews, int viewId, int notificationId, Notification notification, String notificationTag) {
        long started = System.nanoTime();

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
            throw new IllegalArgumentException("Cannot use placeholder or error drawables with remote views.");
        }

        Request request = createRequest(started);
        String key = Utils.createKey(request, new StringBuilder()); // Non-main thread needs own builder.

        RemoteViewsAction action = new RemoteViewsAction.NotificationAction(loader, request, remoteViews, viewId, notificationId, notification, notificationTag, memoryPolicy, networkPolicy, key, tag, errorResId);

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
            throw new IllegalArgumentException("Cannot use placeholder or error drawables with remote views.");
        }

        Request request = createRequest(started);
        String key = Utils.createKey(request, new StringBuilder()); // Non-main thread needs own builder.

        RemoteViewsAction action = new RemoteViewsAction.AppWidgetAction(loader, request, remoteViews, viewId, appWidgetIds, memoryPolicy, networkPolicy, key, tag, errorResId);

        performRemoteViewInto(action);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link ImageView}.
     * <p/>
     * <em>Note:</em> This method keeps a weak reference to the {@link ImageView} instance and will
     * automatically support object recycling.
     */
    public void into(ImageView target) {
        into(target, null);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link ImageView} and invokes the
     * target {@link Callback} if it's not {@code null}.
     * <p/>
     * <em>Note:</em> The {@link Callback} param is a strong reference and will prevent your
     * {@link android.app.Activity} or {@link android.app.Fragment} from being garbage collected. If
     * you use this method, it is <b>strongly</b> recommended you invoke an adjacent
     * {@link EasyLoader#cancelRequest(android.widget.ImageView)} call to prevent temporary leaking.
     */
    public void into(ImageView target, Callback callback) {
        long started = System.nanoTime();
        ThreadChecker.checkMain();

        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }

        if (!data.hasImage()) {
            loader.cancelRequest(target);
            if (setPlaceholder) {
                PicassoDrawable.setPlaceholder(target, getPlaceholderDrawable());
            }
            return;
        }

        if (deferred) {
            if (data.hasSize()) {
                throw new IllegalStateException("Fit cannot be used with resize.");
            }
            int width = target.getWidth();
            int height = target.getHeight();
            if (width == 0 || height == 0) {
                if (setPlaceholder) {
                    PicassoDrawable.setPlaceholder(target, getPlaceholderDrawable());
                }
                loader.defer(target, new DeferredRequestCreator(this, target, callback));
                return;
            }
            data.resize(width, height);
        }

        Request request = createRequest(started);
        String requestKey = Utils.createKey(request);

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            Bitmap bitmap = loader.quickMemoryCacheCheck(requestKey);
            if (bitmap != null) {
                loader.cancelRequest(target);
                PicassoDrawable.setBitmap(target, loader.context, bitmap, LoadedFrom.MEMORY, noFade, loader.indicatorsEnabled);
                if (loader.isLoggingEnabled()) {
                    log(Utils.OWNER_MAIN, Utils.VERB_COMPLETED, request.plainId(), "from " + LoadedFrom.MEMORY);
                }
                if (callback != null) {
                    callback.onSuccess();
                }
                return;
            }
        }

        if (setPlaceholder) {
            PicassoDrawable.setPlaceholder(target, getPlaceholderDrawable());
        }

        Action action = new ImageViewAction(loader, target, request, memoryPolicy, networkPolicy, errorResId, errorDrawable, requestKey, tag, callback, noFade);

        loader.enqueueAndSubmit(action);
    }

    private Drawable getPlaceholderDrawable() {
        if (placeholderResId != 0) {
            return loader.context.getResources().getDrawable(placeholderResId);
        } else {
            return placeholderDrawable; // This may be null which is expected and desired behavior.
        }
    }

    /**
     * Create the request optionally passing it through the request transformer.
     */
    private Request createRequest(long started) {
        int id = nextId.getAndIncrement();

        Request request = data.build();
        request.id = id;
        request.started = started;

        boolean loggingEnabled = loader.isLoggingEnabled();
        if (loggingEnabled) {
            log(Utils.OWNER_MAIN, Utils.VERB_CREATED, request.plainId(), request.toString());
        }

        Request transformed = loader.transformRequest(request);
        if (transformed != request) {
            // If the request was changed, copy over the id and timestamp from the original.
            transformed.id = id;
            transformed.started = started;

            if (loggingEnabled) {
                log(Utils.OWNER_MAIN, Utils.VERB_CHANGED, transformed.logId(), "into " + transformed);
            }
        }

        return transformed;
    }

    private void performRemoteViewInto(RemoteViewsAction action) {
        if (shouldReadFromMemoryCache(memoryPolicy)) {
            Bitmap bitmap = loader.quickMemoryCacheCheck(action.getKey());
            if (bitmap != null) {
                action.complete(bitmap, LoadedFrom.MEMORY);
                return;
            }
        }

        if (placeholderResId != 0) {
            action.setImageResource(placeholderResId);
        }

        loader.enqueueAndSubmit(action);
    }
}
