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
package site.hanschen.easyloader.request;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;

import site.hanschen.easyloader.LoadedFrom;

import java.io.IOException;

import static android.content.ContentResolver.SCHEME_FILE;
import static android.media.ExifInterface.ORIENTATION_NORMAL;
import static android.media.ExifInterface.TAG_ORIENTATION;

public class FileRequestHandler extends ContentStreamRequestHandler {

    public FileRequestHandler(Context context) {
        super(context);
    }

    @Override
    public boolean canHandleRequest(Request request) {
        return (request != null && request.uri != null) && SCHEME_FILE.equals(request.uri.getScheme());
    }

    @Override
    public Result handle(Request request) throws IOException {
        return new Result(null, getInputStream(request), LoadedFrom.DISK, getFileExifRotation(request.uri));
    }

    private static int getFileExifRotation(Uri uri) throws IOException {
        ExifInterface exifInterface = new ExifInterface(uri.getPath());
        return exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
    }
}
