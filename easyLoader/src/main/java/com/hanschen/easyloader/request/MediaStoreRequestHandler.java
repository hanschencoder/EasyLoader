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
package com.hanschen.easyloader.request;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentUris.parseId;
import static android.provider.MediaStore.Images;
import static android.provider.MediaStore.Images.Thumbnails.FULL_SCREEN_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MICRO_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MINI_KIND;
import static com.hanschen.easyloader.LoadedFrom.DISK;
import static com.hanschen.easyloader.request.MediaStoreRequestHandler.Kind.FULL;
import static com.hanschen.easyloader.request.MediaStoreRequestHandler.Kind.MICRO;
import static com.hanschen.easyloader.request.MediaStoreRequestHandler.Kind.MINI;
import static com.hanschen.easyloader.util.BitmapUtils.calculateInSampleSize;
import static com.hanschen.easyloader.util.BitmapUtils.createBitmapOptions;

public class MediaStoreRequestHandler extends ContentStreamRequestHandler {
    private static final String[] CONTENT_ORIENTATION = new String[]{Images.ImageColumns.ORIENTATION};

    public MediaStoreRequestHandler(Context context) {
        super(context);
    }

    @Override
    public boolean canHandleRequest(Request request) {
        if (request == null || request.uri == null) {
            return false;
        }
        final Uri uri = request.uri;
        return (SCHEME_CONTENT.equals(uri.getScheme()) && MediaStore.AUTHORITY.equals(uri.getAuthority()));
    }

    @Override
    public Result handle(Request request) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        int exifOrientation = getExifOrientation(contentResolver, request.uri);

        String mimeType = contentResolver.getType(request.uri);
        boolean isVideo = mimeType != null && mimeType.startsWith("video/");

        if (request.hasSize()) {
            Kind kind = getKind(request.targetWidth, request.targetHeight);
            if (!isVideo && kind == FULL) {
                return new Result(null, getInputStream(request), DISK, exifOrientation);
            }

            long id = parseId(request.uri);

            BitmapFactory.Options options = createBitmapOptions(request);
            options.inJustDecodeBounds = true;

            calculateInSampleSize(request.targetWidth, request.targetHeight, kind.width, kind.height, options, request);

            Bitmap bitmap;

            if (isVideo) {
                // Since MediaStore doesn't provide the full screen kind thumbnail, we use the mini kind
                // instead which is the largest thumbnail size can be fetched from MediaStore.
                int tempKind = (kind == FULL) ? MediaStore.Video.Thumbnails.MINI_KIND : kind.androidKind;
                bitmap = MediaStore.Video.Thumbnails.getThumbnail(contentResolver, id, tempKind, options);
            } else {
                bitmap = Images.Thumbnails.getThumbnail(contentResolver, id, kind.androidKind, options);
            }

            if (bitmap != null) {
                return new Result(bitmap, null, DISK, exifOrientation);
            }
        }

        return new Result(null, getInputStream(request), DISK, exifOrientation);
    }


    private static Kind getKind(int targetWidth, int targetHeight) {
        if (targetWidth <= MICRO.width && targetHeight <= MICRO.height) {
            return MICRO;
        } else if (targetWidth <= MINI.width && targetHeight <= MINI.height) {
            return MINI;
        }
        return FULL;
    }

    private static int getExifOrientation(ContentResolver contentResolver, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, CONTENT_ORIENTATION, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return 0;
            }
            return cursor.getInt(0);
        } catch (RuntimeException ignored) {
            // If the orientation column doesn't exist, assume no rotation.
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    enum Kind {
        MICRO(MICRO_KIND, 96, 96),
        MINI(MINI_KIND, 512, 384),
        FULL(FULL_SCREEN_KIND, -1, -1);

        final int androidKind;
        final int width;
        final int height;

        Kind(int androidKind, int width, int height) {
            this.androidKind = androidKind;
            this.width = width;
            this.height = height;
        }
    }
}
