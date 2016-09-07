/*
 * Copyright (C) 2013 Square, Inc.
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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;

import com.hanschen.easyloader.request.Request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadFactory;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

final public class Utils {
    public static final  String THREAD_PREFIX           = "EasyLoader-";
    public static final  String THREAD_IDLE_NAME        = THREAD_PREFIX + "Idle";
    private static final int    MIN_DISK_CACHE_SIZE     = 5 * 1024 * 1024; // 5MB
    private static final int    MAX_DISK_CACHE_SIZE     = 50 * 1024 * 1024; // 50MB
    static final         int    THREAD_LEAK_CLEANING_MS = 1000;


    /**
     * Logging
     */
    public static final String OWNER_MAIN       = "Main";
    public static final String OWNER_DISPATCHER = "Dispatcher";
    public static final String OWNER_HUNTER     = "Hunter";
    public static final String VERB_CREATED     = "created";
    public static final String VERB_CHANGED     = "changed";
    public static final String VERB_IGNORED     = "ignored";
    public static final String VERB_ENQUEUED    = "enqueued";
    public static final String VERB_CANCELED    = "canceled";
    public static final String VERB_BATCHED     = "batched";
    public static final String VERB_RETRYING    = "retrying";
    public static final String VERB_EXECUTING   = "executing";
    public static final String VERB_DECODED     = "decoded";
    public static final String VERB_TRANSFORMED = "transformed";
    public static final String VERB_JOINED      = "joined";
    public static final String VERB_REMOVED     = "removed";
    public static final String VERB_DELIVERED   = "delivered";
    public static final String VERB_REPLAYING   = "replaying";
    public static final String VERB_COMPLETED   = "completed";
    public static final String VERB_ERRORED     = "errored";
    public static final String VERB_PAUSED      = "paused";
    public static final String VERB_RESUMED     = "resumed";

    /**
     * Thread confined to main thread for key creation.
     */
    static final StringBuilder MAIN_THREAD_KEY_BUILDER = new StringBuilder();


    /* WebP file header
       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |      'R'      |      'I'      |      'F'      |      'F'      |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                           File Size                           |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |      'W'      |      'E'      |      'B'      |      'P'      |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    */
    private static final int    WEBP_FILE_HEADER_SIZE = 12;
    private static final String WEBP_FILE_HEADER_RIFF = "RIFF";
    private static final String WEBP_FILE_HEADER_WEBP = "WEBP";

    private Utils() {
        // No instances.
    }


    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    @TargetApi(JELLY_BEAN_MR2)
    static long calculateDiskCacheSize(File dir) {
        long size = MIN_DISK_CACHE_SIZE;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            //noinspection deprecation
            long blockCount = SDK_INT < JELLY_BEAN_MR2 ? (long) statFs.getBlockCount() : statFs.getBlockCountLong();
            //noinspection deprecation
            long blockSize = SDK_INT < JELLY_BEAN_MR2 ? (long) statFs.getBlockSize() : statFs.getBlockSizeLong();
            long available = blockCount * blockSize;
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

    static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = getService(context, ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB) {
            memoryClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);
        }
        // Target ~15% of the available heap.
        return (int) (1024L * 1024L * memoryClass / 7);
    }

    public static boolean isAirplaneModeOn(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            if (SDK_INT < JELLY_BEAN_MR1) {
                //noinspection deprecation
                return Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
            }
            return Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        } catch (NullPointerException e) {
            // https://github.com/square/picasso/issues/761, some devices might crash here, assume that
            // airplane mode is off.
            return false;
        } catch (SecurityException e) {
            //https://github.com/square/picasso/issues/1197
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getService(Context context, String service) {
        return (T) context.getSystemService(service);
    }

    public static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            byteArrayOutputStream.write(buffer, 0, n);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static boolean isWebPFile(InputStream stream) throws IOException {
        byte[] fileHeaderBytes = new byte[WEBP_FILE_HEADER_SIZE];
        boolean isWebPFile = false;
        if (stream.read(fileHeaderBytes, 0, WEBP_FILE_HEADER_SIZE) == WEBP_FILE_HEADER_SIZE) {
            // If a file's header starts with RIFF and end with WEBP, the file is a WebP file
            isWebPFile = WEBP_FILE_HEADER_RIFF.equals(new String(fileHeaderBytes, 0, 4, "US-ASCII")) && WEBP_FILE_HEADER_WEBP.equals(new String(fileHeaderBytes, 8, 4, "US-ASCII"));
        }
        return isWebPFile;
    }

    /**
     * Prior to Android 5, HandlerThread always keeps a stack local reference to the last message
     * that was sent to it. This method makes sure that stack local reference never stays there
     * for too long by sending new messages to it every second.
     */
    public static void flushStackLocalLeaks(Looper looper) {
        Handler handler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                sendMessageDelayed(obtainMessage(), THREAD_LEAK_CLEANING_MS);
            }
        };
        handler.sendMessageDelayed(handler.obtainMessage(), THREAD_LEAK_CLEANING_MS);
    }

    @TargetApi(HONEYCOMB)
    private static class ActivityManagerHoneycomb {
        static int getLargeMemoryClass(ActivityManager activityManager) {
            return activityManager.getLargeMemoryClass();
        }
    }

    public static class PicassoThreadFactory implements ThreadFactory {
        @SuppressWarnings("NullableProblems")
        public Thread newThread(Runnable r) {
            return new PicassoThread(r);
        }
    }

    private static class PicassoThread extends Thread {
        public PicassoThread(Runnable r) {
            super(r);
        }

        @Override
        public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            super.run();
        }
    }

    @TargetApi(HONEYCOMB_MR1)
    private static class BitmapHoneycombMR1 {
        static int getByteCount(Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    }

    public static String createKey(Request data) {
        String result = createKey(data, MAIN_THREAD_KEY_BUILDER);
        MAIN_THREAD_KEY_BUILDER.setLength(0);
        return result;
    }

    public static String createKey(Request data, StringBuilder builder) {

        final int KEY_PADDING = 50; // Determined by exact science.
        final char KEY_SEPARATOR = '\n';
        if (data.stableKey != null) {
            builder.ensureCapacity(data.stableKey.length() + KEY_PADDING);
            builder.append(data.stableKey);
        } else if (data.uri != null) {
            String path = data.uri.toString();
            builder.ensureCapacity(path.length() + KEY_PADDING);
            builder.append(path);
        } else {
            builder.ensureCapacity(KEY_PADDING);
            builder.append(data.resourceId);
        }
        builder.append(KEY_SEPARATOR);

        if (data.rotationDegrees != 0) {
            builder.append("rotation:").append(data.rotationDegrees);
            if (data.hasRotationPivot) {
                builder.append('@').append(data.rotationPivotX).append('x').append(data.rotationPivotY);
            }
            builder.append(KEY_SEPARATOR);
        }
        if (data.hasSize()) {
            builder.append("resize:").append(data.targetWidth).append('x').append(data.targetHeight);
            builder.append(KEY_SEPARATOR);
        }
        if (data.centerCrop) {
            builder.append("centerCrop").append(KEY_SEPARATOR);
        } else if (data.centerInside) {
            builder.append("centerInside").append(KEY_SEPARATOR);
        }

        if (data.transformations != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, count = data.transformations.size(); i < count; i++) {
                builder.append(data.transformations.get(i).key());
                builder.append(KEY_SEPARATOR);
            }
        }

        return builder.toString();
    }
}
