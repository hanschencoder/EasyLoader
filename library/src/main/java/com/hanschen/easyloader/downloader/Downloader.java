package com.hanschen.easyloader.downloader;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hanschen.easyloader.bean.Response;

import java.io.IOException;

/**
 * Created by Hans.Chen on 2016/7/28.
 */
public interface Downloader {

    @Nullable
    Response load(@NonNull Uri uri) throws IOException;

    void shutdown();
}
