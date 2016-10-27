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
package com.hanschen.easyloader.downloader;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.bean.NetworkResponse;
import com.hanschen.easyloader.log.EasyLoaderLog;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OkHttp3Downloader implements Downloader {

    private final OkHttpClient client;

    public OkHttp3Downloader() {
        client = new OkHttpClient.Builder().build();
    }

    @Override
    public NetworkResponse load(@NonNull Uri uri) throws IOException {
        EasyLoaderLog.d("download", "download: " + uri.toString());
        Request.Builder builder = new Request.Builder().url(uri.toString());
        okhttp3.Response response = client.newCall(builder.build()).execute();
        int responseCode = response.code();
        if (responseCode >= 300) {
            response.body().close();
            throw new ResponseException(responseCode + " " + response.message(), responseCode);
        }
        return new NetworkResponse(response.body().byteStream(), response.body().contentLength());
    }
}
