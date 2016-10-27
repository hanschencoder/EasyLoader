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
package site.hanschen.easyloader.action;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import site.hanschen.easyloader.Callback;
import site.hanschen.easyloader.EasyLoader;
import site.hanschen.easyloader.EnhanceDrawable;
import site.hanschen.easyloader.LoadedFrom;
import site.hanschen.easyloader.request.Request;

public class ImageViewAction extends Action<ImageView> {

    private Callback callback;

    public ImageViewAction(EasyLoader loader,
                           ImageView imageView,
                           Request data,
                           int memoryPolicy,
                           int diskPolicy,
                           int errorResId,
                           Drawable errorDrawable,
                           String key,
                           Object tag,
                           Callback callback,
                           boolean noFade) {
        super(loader, imageView, data, memoryPolicy, diskPolicy, errorResId, errorDrawable, key, tag, noFade);
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

        EnhanceDrawable.setBitmap(target, loader.getContext(), result, from, noFade, loader.isIndicatorsEnabled());

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
