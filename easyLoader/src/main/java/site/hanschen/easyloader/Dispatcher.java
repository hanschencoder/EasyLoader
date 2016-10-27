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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import site.hanschen.easyloader.action.Action;
import site.hanschen.easyloader.cache.CacheManager;
import site.hanschen.easyloader.util.Utils;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static site.hanschen.easyloader.DiskPolicy.shouldWriteToDiskCache;
import static site.hanschen.easyloader.MemoryPolicy.shouldWriteToMemoryCache;
import static site.hanschen.easyloader.util.Utils.getService;

public class Dispatcher {

    private static final int RETRY_DELAY       = 500;
    private static final int AIRPLANE_MODE_ON  = 1;
    private static final int AIRPLANE_MODE_OFF = 0;

    static final int REQUEST_SUBMIT          = 1;
    static final int REQUEST_CANCEL          = 2;
    static final int REQUEST_GCED            = 3;
    static final int HUNTER_SUCCESS          = 4;
    static final int HUNTER_RETRY            = 5;
    static final int HUNTER_DECODE_FAILED    = 6;
    static final int HUNTER_DELAY_NEXT_BATCH = 7;
    static final int HUNTER_BATCH_COMPLETE   = 8;
    static final int NETWORK_STATE_CHANGE    = 9;
    static final int AIRPLANE_MODE_CHANGE    = 10;
    static final int TAG_PAUSE               = 11;
    static final int TAG_RESUME              = 12;
    static final int REQUEST_BATCH_RESUME    = 13;

    private static final String DISPATCHER_THREAD_NAME = "Dispatcher";
    private static final int    BATCH_DELAY            = 200; // ms

    private final DispatcherThread             dispatcherThread;
    private final Context                      context;
    private final AdjustableExecutorService    service;
    /**
     * 已加入请求列表的任务，任务取消、完成或者失败后会移除
     */
    private final Map<String, BitmapHunter>    hunterMap;
    /**
     * 需重新进行请求任务列表，在重新联网的时候，会把当前列表的任务重新进行请求
     */
    private final Map<Object, Action>          failedActions;
    /**
     * 暂停请求的任务列表
     */
    private final Map<Object, Action>          pausedActions;
    /**
     * 已缓存暂未处理的结果
     */
    private final List<BitmapHunter>           batch;
    private final Set<Object>                  pausedTags;
    private final Handler                      dispatcherHandler;
    private final Handler                      mainThreadHandler;
    private final CacheManager<String, Bitmap> memoryCache;
    private final CacheManager<String, Bitmap> diskCache;
    private final NetworkBroadcastReceiver     receiver;
    private final boolean                      canScansNetworkChanges;
    private       boolean                      airplaneMode;

    Dispatcher(Context context,
               AdjustableExecutorService service,
               Handler mainThreadHandler,
               CacheManager<String, Bitmap> memoryCache,
               CacheManager<String, Bitmap> diskCache) {
        this.dispatcherThread = new DispatcherThread();
        this.dispatcherThread.start();
        Utils.flushStackLocalLeaks(dispatcherThread.getLooper());
        this.dispatcherHandler = new DispatcherHandler(dispatcherThread.getLooper(), this);

        this.context = context;
        this.service = service;
        this.hunterMap = new LinkedHashMap<>();
        this.failedActions = new WeakHashMap<>();
        this.pausedActions = new WeakHashMap<>();
        this.pausedTags = new HashSet<>();
        this.mainThreadHandler = mainThreadHandler;
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
        this.batch = new ArrayList<>(4);
        this.airplaneMode = Utils.isAirplaneModeOn(this.context);
        this.canScansNetworkChanges = Utils.hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
        this.receiver = new NetworkBroadcastReceiver(this);
        receiver.register();
    }

    void shutdown() {
        service.shutdown();
        dispatcherThread.quit();
        EasyLoader.HANDLER.post(new Runnable() {
            @Override
            public void run() {
                receiver.unregister();
            }
        });
    }

    /**
     * 提交Action
     */
    void dispatchSubmit(Action action) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_SUBMIT, action));
    }

    /**
     * 取消Action
     */
    void dispatchCancel(Action action) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(REQUEST_CANCEL, action));
    }

    void dispatchPauseTag(Object tag) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(TAG_PAUSE, tag));
    }

    void dispatchResumeTag(Object tag) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(TAG_RESUME, tag));
    }

    /**
     * 请求完成
     *
     * @param hunter 包含result结果的hunter
     */
    void dispatchSuccess(BitmapHunter hunter) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(HUNTER_SUCCESS, hunter));
    }

    /**
     * 发生IOException，重新尝试
     */
    void dispatchRetry(BitmapHunter hunter) {
        dispatcherHandler.sendMessageDelayed(dispatcherHandler.obtainMessage(HUNTER_RETRY, hunter), RETRY_DELAY);
    }

    /**
     * 请求失败
     */
    void dispatchFailed(BitmapHunter hunter) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(HUNTER_DECODE_FAILED, hunter));
    }

    /**
     * 网络状态变化
     */
    private void dispatchNetworkStateChange(NetworkInfo info) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(NETWORK_STATE_CHANGE, info));
    }

    /**
     * 飞行模式状态变化
     */
    private void dispatchAirplaneModeChange(boolean airplaneMode) {
        dispatcherHandler.sendMessage(dispatcherHandler.obtainMessage(AIRPLANE_MODE_CHANGE,
                                                                      airplaneMode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF,
                                                                      0));
    }

    /**
     * 执行Action提交任务
     */
    private void performSubmit(Action action) {
        performSubmit(action, true);
    }

    /**
     * 执行Action提交任务
     *
     * @param action        提交的Action
     * @param dismissFailed 是否从failedActions列表中移除
     */
    private void performSubmit(Action action, boolean dismissFailed) {
        if (pausedTags.contains(action.getTag())) {
            pausedActions.put(action.getTarget(), action);
            return;
        }

        //请求相同，直接attach到已有的hunter中
        BitmapHunter hunter = hunterMap.get(action.getKey());
        if (hunter != null) {
            hunter.attach(action);
            return;
        }

        if (service.isShutdown()) {
            return;
        }

        hunter = BitmapHunter.forRequest(action.getLoader(), this, memoryCache, diskCache, action);
        hunter.setFuture(service.submit(hunter));
        hunterMap.put(action.getKey(), hunter);
        //其实只要执行了performSubmit，就需要从failedActions移除，但是有些调用的地方，会在方法调用之外通过迭代器移除，不需要在这里移除
        if (dismissFailed) {
            failedActions.remove(action.getTarget());
        }
    }

    /**
     * 执行取消Action
     */
    private void performCancel(Action action) {
        String key = action.getKey();
        BitmapHunter hunter = hunterMap.get(key);
        if (hunter != null) {
            hunter.detach(action);
            if (hunter.cancel()) {
                hunterMap.remove(key);
            }
        }

        if (pausedTags.contains(action.getTag())) {
            pausedActions.remove(action.getTarget());
        }

        failedActions.remove(action.getTarget());
    }

    private void performPauseTag(Object tag) {
        // Trying to pause a tag that is already paused.
        if (!pausedTags.add(tag)) {
            return;
        }

        // Go through all active hunters and detach/pause the requests
        // that have the paused tag.
        for (Iterator<BitmapHunter> it = hunterMap.values().iterator(); it.hasNext(); ) {
            BitmapHunter hunter = it.next();

            Action single = hunter.getAction();
            List<Action> joined = hunter.getActions();
            boolean hasMultiple = joined != null && !joined.isEmpty();

            // Hunter has no requests, bail early.
            if (single == null && !hasMultiple) {
                continue;
            }

            if (single != null && single.getTag().equals(tag)) {
                hunter.detach(single);
                pausedActions.put(single.getTarget(), single);
            }

            if (hasMultiple) {
                for (int i = joined.size() - 1; i >= 0; i--) {
                    Action action = joined.get(i);
                    if (!action.getTag().equals(tag)) {
                        continue;
                    }

                    hunter.detach(action);
                    pausedActions.put(action.getTarget(), action);
                }
            }

            // Check if the hunter can be cancelled in case all its requests
            // had the tag being paused here.
            if (hunter.cancel()) {
                it.remove();
            }
        }
    }

    private void performResumeTag(Object tag) {
        // Trying to resume a tag that is not paused.
        if (!pausedTags.remove(tag)) {
            return;
        }

        List<Action> batch = null;
        for (Iterator<Action> i = pausedActions.values().iterator(); i.hasNext(); ) {
            Action action = i.next();
            if (action.getTag().equals(tag)) {
                if (batch == null) {
                    batch = new ArrayList<Action>();
                }
                batch.add(action);
                i.remove();
            }
        }

        if (batch != null) {
            mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(REQUEST_BATCH_RESUME, batch));
        }
    }


    /**
     * 执行重新尝试
     */
    private void performRetry(BitmapHunter hunter) {
        if (hunter.isCancelled()) {
            return;
        }

        if (service.isShutdown()) {
            performError(hunter, false);
            return;
        }

        NetworkInfo networkInfo = null;
        if (canScansNetworkChanges) {
            ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }

        boolean hasConnectivity = networkInfo != null && networkInfo.isConnected();
        boolean supportsReplay = hunter.supportsReplay();

        boolean shouldRetryHunter = hunter.shouldRetry(airplaneMode, networkInfo);
        if (!shouldRetryHunter) {
            boolean willReplay = canScansNetworkChanges && supportsReplay;
            performError(hunter, willReplay);
            if (willReplay) {
                markForReplay(hunter);
            }
            return;
        }

        if (!canScansNetworkChanges || hasConnectivity) {
            hunter.setFuture(service.submit(hunter));
            return;
        }

        performError(hunter, supportsReplay);
        if (supportsReplay) {
            markForReplay(hunter);
        }
    }

    /**
     * 执行成功后的处理，写入cache，加入批处理结果
     */
    private void performSuccess(BitmapHunter hunter) {
        if (shouldWriteToMemoryCache(hunter.getMemoryPolicy())) {
            memoryCache.put(hunter.getKey(), hunter.getResult());
        }
        if (shouldWriteToDiskCache(hunter.getDiskPolicy())) {
            diskCache.put(hunter.getKey(), hunter.getResult());
        }
        hunterMap.remove(hunter.getKey());
        batch(hunter);
    }

    /**
     * 执行批处理
     */
    private void performBatchComplete() {
        List<BitmapHunter> copy = new ArrayList<>(batch);
        batch.clear();
        mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(HUNTER_BATCH_COMPLETE, copy));
    }

    /**
     * 执行失败后批处理
     *
     * @param willReplay 是否会加入联网重新请求列表
     */
    private void performError(BitmapHunter hunter, boolean willReplay) {
        hunterMap.remove(hunter.getKey());
        batch(hunter);
    }

    /**
     * 设置新的飞行模式状态
     */
    private void performAirplaneModeChange(boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    /**
     * 网络状态变化,调整线程池大小
     */
    private void performNetworkStateChange(NetworkInfo info) {
        service.adjustThreadCount(info);
        if (info != null && info.isConnected()) {
            flushFailedActions();
        }
    }

    /**
     * 重新提交失败的任务
     */
    private void flushFailedActions() {
        if (!failedActions.isEmpty()) {
            Iterator<Action> iterator = failedActions.values().iterator();
            while (iterator.hasNext()) {
                Action action = iterator.next();
                iterator.remove();
                performSubmit(action, false);
            }
        }
    }

    /**
     * 加入联网重试列表
     */
    private void markForReplay(BitmapHunter hunter) {
        Action action = hunter.getAction();
        if (action != null) {
            markForReplay(action);
        }
        List<Action> joined = hunter.getActions();
        if (joined != null) {
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                markForReplay(join);
            }
        }
    }

    /**
     * 放入联网重试列表，重新联网的时候会重新发起请求
     */
    private void markForReplay(Action action) {
        Object target = action.getTarget();
        if (target != null) {
            action.setWillReplay(true);
            failedActions.put(target, action);
        }
    }

    /**
     * 将待处理(成功或失败)的hunter放入列表中，缓存200ms后统一处理
     */
    private void batch(BitmapHunter hunter) {
        if (hunter.isCancelled()) {
            return;
        }
        batch.add(hunter);
        if (!dispatcherHandler.hasMessages(HUNTER_DELAY_NEXT_BATCH)) {
            dispatcherHandler.sendEmptyMessageDelayed(HUNTER_DELAY_NEXT_BATCH, BATCH_DELAY);
        }
    }

    private static class DispatcherHandler extends Handler {

        private final Dispatcher dispatcher;

        DispatcherHandler(Looper looper, Dispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REQUEST_SUBMIT: {
                    Action action = (Action) msg.obj;
                    dispatcher.performSubmit(action);
                    break;
                }
                case REQUEST_CANCEL: {
                    Action action = (Action) msg.obj;
                    dispatcher.performCancel(action);
                    break;
                }
                case TAG_PAUSE: {
                    Object tag = msg.obj;
                    dispatcher.performPauseTag(tag);
                    break;
                }
                case TAG_RESUME: {
                    Object tag = msg.obj;
                    dispatcher.performResumeTag(tag);
                    break;
                }
                case HUNTER_SUCCESS: {
                    BitmapHunter hunter = (BitmapHunter) msg.obj;
                    dispatcher.performSuccess(hunter);
                    break;
                }
                case HUNTER_RETRY: {
                    BitmapHunter hunter = (BitmapHunter) msg.obj;
                    dispatcher.performRetry(hunter);
                    break;
                }
                case HUNTER_DECODE_FAILED: {
                    BitmapHunter hunter = (BitmapHunter) msg.obj;
                    dispatcher.performError(hunter, false);
                    break;
                }
                case HUNTER_DELAY_NEXT_BATCH: {
                    dispatcher.performBatchComplete();
                    break;
                }
                case NETWORK_STATE_CHANGE: {
                    NetworkInfo info = (NetworkInfo) msg.obj;
                    dispatcher.performNetworkStateChange(info);
                    break;
                }
                case AIRPLANE_MODE_CHANGE: {
                    dispatcher.performAirplaneModeChange(msg.arg1 == AIRPLANE_MODE_ON);
                    break;
                }
                default:
                    EasyLoader.HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            throw new AssertionError("Unknown dispatcherHandler message received: " + msg.what);
                        }
                    });
            }
        }
    }

    private static class DispatcherThread extends HandlerThread {
        DispatcherThread() {
            super(Utils.THREAD_PREFIX + DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        }
    }

    /**
     * 网络监听广播接收器
     */
    static class NetworkBroadcastReceiver extends BroadcastReceiver {

        static final String EXTRA_AIRPLANE_STATE = "state";
        private final Dispatcher dispatcher;

        NetworkBroadcastReceiver(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_AIRPLANE_MODE_CHANGED);
            if (dispatcher.canScansNetworkChanges) {
                filter.addAction(CONNECTIVITY_ACTION);
            }
            dispatcher.context.registerReceiver(this, filter);
        }

        void unregister() {
            dispatcher.context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();
            if (ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                //飞行模式状态变化
                if (!intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
                    return;
                }
                dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
            } else if (CONNECTIVITY_ACTION.equals(action)) {
                //网络状态变化
                ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
                dispatcher.dispatchNetworkStateChange(connectivityManager.getActiveNetworkInfo());
            }
        }
    }
}
