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

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * execute in background thread.
 */
public final class BackgroundPoster {


    private Executor mPoster;

    public BackgroundPoster(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        mPoster = new ThreadPoolExecutor(corePoolSize,
                                         maximumPoolSize,
                                         keepAliveTime,
                                         TimeUnit.SECONDS,
                                         new LinkedBlockingQueue<Runnable>());
    }

    public void post(@NonNull Runnable r) {
        mPoster.execute(r);
    }
}
