package com.hanschen.easyloader.callback;

import android.net.Uri;

import com.hanschen.easyloader.EasyLoader;

/**
 * Created by Hans.Chen on 2016/7/28.
 */
public interface OnLoadListener {

    void onLoadStart(Uri uri);

    void onProgress(Uri uri, int progress);

    void onLoadSuccessful(Uri uri);

    void onLoadFailed(EasyLoader loader, Uri uri, Exception exception);
}
