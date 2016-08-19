package com.hanschen.easyloader.downloader;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.bean.Response;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * Created by Hans on 2016/8/19.
 */
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
    public Response load(@NonNull Uri uri) throws IOException {
        Request.Builder builder = new Request.Builder().url(uri.toString());
        com.squareup.okhttp.Response response = client.newCall(builder.build()).execute();
        int responseCode = response.code();
        if (responseCode >= 300) {
            response.body().close();
            throw new ResponseException(responseCode + " " + response.message(), responseCode);
        }
        return new Response(response.body().byteStream(), response.body().contentLength());
    }
}
