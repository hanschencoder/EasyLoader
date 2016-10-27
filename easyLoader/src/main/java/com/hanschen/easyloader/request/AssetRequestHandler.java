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
package com.hanschen.easyloader.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import com.hanschen.easyloader.LoadedFrom;

import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentResolver.SCHEME_FILE;


public class AssetRequestHandler extends RequestHandler {

    private static final String ANDROID_ASSET       = "android_asset";
    private static final int    ASSET_PREFIX_LENGTH = (SCHEME_FILE + ":///" + ANDROID_ASSET + "/").length();

    private final Context context;
    private final Object lock = new Object();
    private AssetManager assetManager;

    public AssetRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request request) {
        if (request == null || request.uri == null) {
            return false;
        }
        Uri uri = request.uri;
        return (SCHEME_FILE.equals(uri.getScheme()) && !uri.getPathSegments()
                                                           .isEmpty() && ANDROID_ASSET.equals(uri.getPathSegments().get(0)));
    }

    @Override
    public Result handle(Request request) throws IOException {
        if (assetManager == null) {
            synchronized (lock) {
                if (assetManager == null) {
                    assetManager = context.getAssets();
                }
            }
        }
        InputStream is = assetManager.open(getFilePath(request));
        return new Result(is, LoadedFrom.DISK);
    }

    private static String getFilePath(Request request) {
        return request.uri.toString().substring(ASSET_PREFIX_LENGTH);
    }
}
