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

import android.content.ContentResolver;
import android.content.Context;

import com.hanschen.easyloader.LoadedFrom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentResolver.SCHEME_CONTENT;

public class ContentStreamRequestHandler extends RequestHandler {

    final Context context;

    public ContentStreamRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request request) {
        return (request != null && request.uri != null) && SCHEME_CONTENT.equals(request.uri.getScheme());
    }

    @Override
    public Result handle(Request request) throws IOException {
        return new Result(getInputStream(request), LoadedFrom.DISK);
    }

    InputStream getInputStream(Request request) throws FileNotFoundException {
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.openInputStream(request.uri);
    }
}
