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