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
package com.hanschen.easyloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.hanschen.easyloader.request.Request;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.KITKAT;

public class BitmapUtils {

    public static int getBitmapBytes(Bitmap bitmap) {
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

    /**
     * Lazily create {@link BitmapFactory.Options} based in given
     * {@link Request}, only instantiating them if needed.
     */
    public static BitmapFactory.Options createBitmapOptions(Request data) {
        final boolean justBounds = data.hasSize();
        final boolean hasConfig = data.config != null;
        BitmapFactory.Options options = null;
        if (justBounds || hasConfig || data.purgeable) {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = justBounds;
            options.inInputShareable = data.purgeable;
            options.inPurgeable = data.purgeable;
            if (hasConfig) {
                options.inPreferredConfig = data.config;
            }
        }
        return options;
    }

    public static boolean requiresInSampleSize(BitmapFactory.Options options) {
        return options != null && options.inJustDecodeBounds;
    }

    public static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options, Request request) {
        calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options, request);
    }

    public static void calculateInSampleSize(int reqWidth,
                                             int reqHeight,
                                             int width,
                                             int height,
                                             BitmapFactory.Options options,
                                             Request request) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio;
            final int widthRatio;
            if (reqHeight == 0) {
                sampleSize = (int) Math.floor((float) width / (float) reqWidth);
            } else if (reqWidth == 0) {
                sampleSize = (int) Math.floor((float) height / (float) reqHeight);
            } else {
                heightRatio = (int) Math.floor((float) height / (float) reqHeight);
                widthRatio = (int) Math.floor((float) width / (float) reqWidth);
                sampleSize = request.centerInside ? Math.max(heightRatio, widthRatio) : Math.min(heightRatio, widthRatio);
            }
        }
        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
    }
}
