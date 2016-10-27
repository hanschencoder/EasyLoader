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
package site.hanschen.easyloader.downloader;

import android.net.Uri;
import android.support.annotation.NonNull;

import site.hanschen.easyloader.bean.NetworkResponse;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkhttpDownloader implements Downloader {

    static final int DEFAULT_READ_TIMEOUT_SECOND    = 20; // 20s
    static final int DEFAULT_WRITE_TIMEOUT_SECOND   = 20; // 20s
    static final int DEFAULT_CONNECT_TIMEOUT_SECOND = 15; // 15s

    private final OkHttpClient client;

    public OkhttpDownloader() {
        client = new OkHttpClient();
        client.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_SECOND, TimeUnit.SECONDS);
        client.setReadTimeout(DEFAULT_READ_TIMEOUT_SECOND, TimeUnit.SECONDS);
        client.setWriteTimeout(DEFAULT_WRITE_TIMEOUT_SECOND, TimeUnit.SECONDS);
    }

    @Override
    public NetworkResponse load(@NonNull Uri uri) throws IOException {
        Request.Builder builder = new Request.Builder().url(uri.toString());
        com.squareup.okhttp.Response response = client.newCall(builder.build()).execute();
        int responseCode = response.code();
        if (responseCode >= 300) {
            response.body().close();
            throw new ResponseException(responseCode + " " + response.message(), responseCode);
        }
        return new NetworkResponse(response.body().byteStream(), response.body().contentLength());
    }
}
