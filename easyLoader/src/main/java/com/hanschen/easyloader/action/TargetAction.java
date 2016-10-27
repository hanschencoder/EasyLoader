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
package com.hanschen.easyloader.action;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.Target;
import com.hanschen.easyloader.request.Request;

public final class TargetAction extends Action<Target> {

    public TargetAction(EasyLoader loader,
                        Target target,
                        Request data,
                        int memoryPolicy,
                        int diskPolicy,
                        Drawable errorDrawable,
                        String key,
                        Object tag,
                        int errorResId) {
        super(loader, target, data, memoryPolicy, diskPolicy, errorResId, errorDrawable, key, tag, false);
    }

    @Override
    public void onComplete(Bitmap result, LoadedFrom from) {
        if (result == null) {
            throw new AssertionError(String.format("Attempted to onComplete action with no result!\n%s", this));
        }
        Target target = getTarget();
        if (target != null) {
            target.onBitmapLoaded(result, from);
            if (result.isRecycled()) {
                throw new IllegalStateException("Target callback must not recycle bitmap!");
            }
        }
    }

    @Override
    public void onError() {
        Target target = getTarget();
        if (target != null) {
            if (errorResId != 0) {
                target.onBitmapFailed(loader.getContext().getResources().getDrawable(errorResId));
            } else {
                target.onBitmapFailed(errorDrawable);
            }
        }
    }
}
