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
package com.hanschen.easyloader;

import android.content.Context;
import android.graphics.Bitmap;

import com.hanschen.easyloader.cache.CacheManager;
import com.hanschen.easyloader.request.RequestHandler;

import java.lang.ref.ReferenceQueue;
import java.util.List;

interface Provider {

    Context getContext();

    ReferenceQueue<Object> getReferenceQueue();

    List<RequestHandler> getRequestHandlers();

    CacheManager<String, Bitmap> getMemoryCacheManager();

    CacheManager<String, Bitmap> getDiskCacheManager();

    Bitmap.Config getDefaultBitmapConfig();

    QueueProcessType getQueueProcessType();

    boolean isShutdown();

    boolean isIndicatorsEnabled();
}
