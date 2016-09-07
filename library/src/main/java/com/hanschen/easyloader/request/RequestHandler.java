package com.hanschen.easyloader.request;

import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Created by Hans on 2016/8/19.
 */
public abstract class RequestHandler {

    /**
     * Whether or not this {@link RequestHandler} can handle a request with the given {@link Request}.
     */
    public abstract boolean canHandleRequest(Request request);

    /**
     * Loads an image for the given {@link Request}.
     *
     * @param request the data from which the image should be resolved.
     */
    @Nullable
    public abstract Result handle(Request request) throws IOException;

    public int getRetryCount() {
        return 0;
    }

    public boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
        return false;
    }

    public boolean supportsReplay() {
        return false;
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
