package com.hanschen.easyloader.downloader;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.bean.NetworkResponse;
import com.hanschen.easyloader.log.EasyLoaderLog;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by Hans on 2016/8/19.
 */
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
