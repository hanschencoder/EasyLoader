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
package com.hanschen.easyloader.request;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static android.content.ContentResolver.SCHEME_ANDROID_RESOURCE;
import static com.hanschen.easyloader.LoadedFrom.DISK;
import static com.hanschen.easyloader.util.BitmapUtils.calculateInSampleSize;
import static com.hanschen.easyloader.util.BitmapUtils.createBitmapOptions;
import static com.hanschen.easyloader.util.BitmapUtils.requiresInSampleSize;

public class ResourceRequestHandler extends RequestHandler {
    private final Context context;

    public ResourceRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request request) {
        return request.resourceId != 0 || SCHEME_ANDROID_RESOURCE.equals(request.uri.getScheme());

    }

    @Override
    public Result handle(Request request) throws IOException {
        Resources res = getResources(context, request);
        int id = getResourceId(res, request);
        return new Result(decodeResource(res, id, request), DISK);
    }

    private static Bitmap decodeResource(Resources resources, int id, Request data) {
        final BitmapFactory.Options options = createBitmapOptions(data);
        if (requiresInSampleSize(options)) {
            BitmapFactory.decodeResource(resources, id, options);
            calculateInSampleSize(data.targetWidth, data.targetHeight, options, data);
        }
        return BitmapFactory.decodeResource(resources, id, options);
    }

    private static int getResourceId(Resources resources, Request data) throws FileNotFoundException {
        if (data.resourceId != 0 || data.uri == null) {
            return data.resourceId;
        }

        String pkg = data.uri.getAuthority();
        if (pkg == null)
            throw new FileNotFoundException("No package provided: " + data.uri);

        int id;
        List<String> segments = data.uri.getPathSegments();
        if (segments == null || segments.isEmpty()) {
            throw new FileNotFoundException("No path segments: " + data.uri);
        } else if (segments.size() == 1) {
            try {
                id = Integer.parseInt(segments.get(0));
            } catch (NumberFormatException e) {
                throw new FileNotFoundException("Last path segment is not a resource ID: " + data.uri);
            }
        } else if (segments.size() == 2) {
            String type = segments.get(0);
            String name = segments.get(1);

            id = resources.getIdentifier(name, type, pkg);
        } else {
            throw new FileNotFoundException("More than two path segments: " + data.uri);
        }
        return id;
    }

    private static Resources getResources(Context context, Request data) throws FileNotFoundException {
        if (data.resourceId != 0 || data.uri == null) {
            return context.getResources();
        }

        String pkg = data.uri.getAuthority();
        if (pkg == null)
            throw new FileNotFoundException("No package provided: " + data.uri);
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getResourcesForApplication(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            throw new FileNotFoundException("Unable to obtain resources for package: " + data.uri);
        }
    }
}
