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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.hanschen.easyloader.Callback;
import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.PicassoDrawable;
import com.hanschen.easyloader.request.Request;

public class ImageViewAction extends Action<ImageView> {

    Callback callback;

    public ImageViewAction(EasyLoader picasso,
                           ImageView imageView,
                           Request data,
                           int memoryPolicy,
                           int networkPolicy,
                           int errorResId,
                           Drawable errorDrawable,
                           String key,
                           Object tag,
                           Callback callback,
                           boolean noFade) {
        super(picasso, imageView, data, memoryPolicy, networkPolicy, errorResId, errorDrawable, key, tag, noFade);
        this.callback = callback;
    }

    @Override
    public void onComplete(Bitmap result, LoadedFrom from) {
        if (result == null) {
            throw new AssertionError(String.format("Attempted to onComplete action with no result!\n%s", this));
        }

        ImageView target = this.target.get();
        if (target == null) {
            return;
        }

        Context context = loader.context;
        boolean indicatorsEnabled = loader.indicatorsEnabled;
        PicassoDrawable.setBitmap(target, context, result, from, noFade, indicatorsEnabled);

        if (callback != null) {
            callback.onSuccess();
        }
    }

    @Override
    public void onError() {
        ImageView target = this.target.get();
        if (target == null) {
            return;
        }
        Drawable placeholder = target.getDrawable();
        if (placeholder instanceof AnimationDrawable) {
            ((AnimationDrawable) placeholder).stop();
        }
        if (errorResId != 0) {
            target.setImageResource(errorResId);
        } else if (errorDrawable != null) {
            target.setImageDrawable(errorDrawable);
        }

        if (callback != null) {
            callback.onError();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if (callback != null) {
            callback = null;
        }
    }
}
