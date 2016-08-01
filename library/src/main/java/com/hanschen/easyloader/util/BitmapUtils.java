package com.hanschen.easyloader.util;

import android.graphics.Bitmap;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public class BitmapUtils {

    static int getBitmapBytes(Bitmap bitmap) {
        int result;
        if (SDK_INT >= KITKAT) {
            result = bitmap.getAllocationByteCount();
        } else if (SDK_INT >= HONEYCOMB_MR1) {
            result = bitmap.getByteCount();
        } else {
            result = bitmap.getRowBytes() * bitmap.getHeight();
        }
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + bitmap);
        }
        return result;
    }
}
