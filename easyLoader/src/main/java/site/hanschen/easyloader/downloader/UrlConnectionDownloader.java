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

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import site.hanschen.easyloader.bean.NetworkResponse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A {@link Downloader} which uses {@link HttpURLConnection} to download images. A disk cache of 2%
 * of the total available space will be used (capped at 50MB) will automatically be installed in the
 * application's cache directory, when available.
 */
public class UrlConnectionDownloader implements Downloader {

    private final Context context;

    public UrlConnectionDownloader(Context context) {
        this.context = context.getApplicationContext();
    }

    protected HttpURLConnection openConnection(Uri path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(path.toString()).openConnection();
        connection.setConnectTimeout(15 * 1000);
        connection.setReadTimeout(20 * 1000);
        return connection;
    }

    @Override
    public NetworkResponse load(@NonNull Uri uri) throws IOException {

        HttpURLConnection connection = openConnection(uri);
        int responseCode = connection.getResponseCode();
        if (responseCode >= 300) {
            connection.disconnect();
            throw new ResponseException(responseCode + " " + connection.getResponseMessage(), responseCode);
        }

        long contentLength = connection.getHeaderFieldInt("Content-Length", -1);
        return new NetworkResponse(connection.getInputStream(), contentLength);
    }
}
