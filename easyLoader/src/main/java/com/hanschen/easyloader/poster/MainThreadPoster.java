package com.hanschen.easyloader.poster;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * execute in main thread.
 */
public final class MainThreadPoster {

    private static MainThreadPoster sInstance;

    private final Handler  mHandler;
    private final Executor mPoster;

    public static MainThreadPoster getInstance() {
        if (sInstance == null) {
            synchronized (MainThreadPoster.class) {
                if (sInstance == null) {
                    sInstance = new MainThreadPoster();
                }
            }
        }
        return sInstance;
    }

    private MainThreadPoster() {
        mHandler = new Handler(Looper.getMainLooper());
        mPoster = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                mHandler.post(command);
            }
        };
    }

    public void post(Runnable r) {
        mPoster.execute(r);
    }

    public void sendMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    public final Message obtainMessage(int what, Object obj) {
        return mHandler.obtainMessage(what, obj);
    }
}