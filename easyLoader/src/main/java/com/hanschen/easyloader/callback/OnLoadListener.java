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
package com.hanschen.easyloader.callback;

import android.net.Uri;

import com.hanschen.easyloader.EasyLoader;

public interface OnLoadListener {

    void onLoadStart(Uri uri);

    void onProgress(Uri uri, int progress);

    void onLoadSuccessful(Uri uri);

    void onLoadFailed(EasyLoader loader, Uri uri, Exception exception);
}
