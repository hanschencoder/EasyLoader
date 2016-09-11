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

import com.hanschen.easyloader.Callback;
import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.request.Request;

public class FetchAction extends Action<Object> {

    private final Object   target;
    private       Callback callback;

    public FetchAction(EasyLoader picasso,
                       Request data,
                       int memoryPolicy,
                       int networkPolicy,
                       Object tag,
                       String key,
                       Callback callback) {
        super(picasso, null, data, memoryPolicy, networkPolicy, 0, null, key, tag, false);
        this.target = new Object();
        this.callback = callback;
    }

    @Override
    public void complete(Bitmap result, LoadedFrom from) {
        if (callback != null) {
            callback.onSuccess();
        }
    }

    @Override
    public void error() {
        if (callback != null) {
            callback.onError();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        callback = null;
    }

    @Override
    public Object getTarget() {
        return target;
    }
}
