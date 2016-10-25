/*
 * Copyright (C) 2013 Square, Inc.
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
package com.hanschen.easyloader.action;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.Priority;
import com.hanschen.easyloader.request.Request;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public abstract class Action<T> {

    public static class RequestWeakReference<T> extends WeakReference<T> {
        private final Action action;

        RequestWeakReference(T referent, ReferenceQueue<? super T> q, Action action) {
            super(referent, q);
            this.action = action;
        }

        public Action getAction() {
            return action;
        }
    }

    public final EasyLoader       loader;
    public final Request          request;
    final        WeakReference<T> target;
    final        boolean          noFade;
    final        int              memoryPolicy;
    final        int              diskPolicy;
    final        int              errorResId;
    final        Drawable         errorDrawable;
    final        String           key;
    final        Object           tag;

    boolean willReplay;
    boolean cancelled;

    Action(EasyLoader loader,
           T target,
           Request request,
           int memoryPolicy,
           int diskPolicy,
           int errorResId,
           Drawable errorDrawable,
           String key,
           Object tag,
           boolean noFade) {
        this.loader = loader;
        this.request = request;
        this.target = target == null ? null : new RequestWeakReference<>(target, loader.getReferenceQueue(), this);
        this.memoryPolicy = memoryPolicy;
        this.diskPolicy = diskPolicy;
        this.noFade = noFade;
        this.errorResId = errorResId;
        this.errorDrawable = errorDrawable;
        this.key = key;
        this.tag = (tag != null ? tag : this);
    }

    public abstract void onComplete(Bitmap result, LoadedFrom from);

    public abstract void onError();

    public void cancel() {
        cancelled = true;
    }

    public Request getRequest() {
        return request;
    }

    public T getTarget() {
        return target == null ? null : target.get();
    }

    public String getKey() {
        return key;
    }

    public boolean isCancelled() {
        return cancelled;
    }


    public void setWillReplay(boolean willReplay) {
        this.willReplay = willReplay;
    }

    public boolean willReplay() {
        return willReplay;
    }

    public int getMemoryPolicy() {
        return memoryPolicy;
    }

    public int getDiskPolicy() {
        return diskPolicy;
    }

    public EasyLoader getLoader() {
        return loader;
    }

    public Priority getPriority() {
        return request.priority;
    }

    public Object getTag() {
        return tag;
    }
}
