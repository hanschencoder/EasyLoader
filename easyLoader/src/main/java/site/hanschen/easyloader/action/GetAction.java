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

import site.hanschen.easyloader.EasyLoader;
import site.hanschen.easyloader.LoadedFrom;
import site.hanschen.easyloader.request.Request;

public class GetAction extends Action<Void> {
    public GetAction(EasyLoader loader, Request data, int memoryPolicy, int diskPolicy, Object tag, String key) {
        super(loader, null, data, memoryPolicy, diskPolicy, 0, null, key, tag, false);
    }

    @Override
    public void onComplete(Bitmap result, LoadedFrom from) {
        //do nothing
    }

    @Override
    public void onError() {
        //do nothing
    }
}
