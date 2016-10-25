package com.hanschen.easyloader.request;

import android.graphics.Bitmap;

import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.util.Utils;

import java.io.InputStream;

/**
 * Created by Hans on 2016/8/19.
 */
public class Result {

    private final LoadedFrom  loadedFrom;
    private final Bitmap      bitmap;
    private final InputStream stream;
    private final int         exifOrientation;

    Result(Bitmap bitmap, LoadedFrom loadedFrom) {
        this(Utils.checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, 0);
    }

    Result(InputStream stream, LoadedFrom loadedFrom) {
        this(null, Utils.checkNotNull(stream, "stream == null"), loadedFrom, 0);
    }

    Result(Bitmap bitmap, InputStream stream, LoadedFrom loadedFrom, int exifOrientation) {
        //bitmap或者stream必须一个为空，一个非空
        if ((bitmap != null) == (stream != null)) {
            throw new AssertionError();
        }
        this.bitmap = bitmap;
        this.stream = stream;
        this.loadedFrom = Utils.checkNotNull(loadedFrom, "loadedFrom == null");
        this.exifOrientation = exifOrientation;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public InputStream getStream() {
        return stream;
    }

    public LoadedFrom getLoadedFrom() {
        return loadedFrom;
    }

    public int getExifOrientation() {
        return exifOrientation;
    }
}
