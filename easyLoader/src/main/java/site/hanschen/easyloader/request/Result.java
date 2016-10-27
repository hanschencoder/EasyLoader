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

import android.graphics.Bitmap;

import site.hanschen.easyloader.LoadedFrom;
import site.hanschen.easyloader.util.Utils;

import java.io.InputStream;

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
