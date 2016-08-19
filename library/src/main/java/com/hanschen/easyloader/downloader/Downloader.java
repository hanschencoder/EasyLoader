package com.hanschen.easyloader.downloader;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.hanschen.easyloader.bean.Response;

import java.io.IOException;

/**
 * Created by Hans.Chen on 2016/7/28.
 */
public interface Downloader {

    Response load(@NonNull Uri uri) throws IOException;
}
