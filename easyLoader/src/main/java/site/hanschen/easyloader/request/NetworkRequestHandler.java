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

import site.hanschen.easyloader.LoadedFrom;
import site.hanschen.easyloader.bean.NetworkResponse;
import site.hanschen.easyloader.downloader.Downloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Hans.Chen
 */
public class NetworkRequestHandler extends RequestHandler {

    private static final String SCHEME_HTTP  = "http";
    private static final String SCHEME_HTTPS = "https";

    private final Downloader downloader;

    public NetworkRequestHandler(Downloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean canHandleRequest(Request request) {
        if (request != null && request.uri != null) {
            String scheme = request.uri.getScheme();
            return (SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme));
        }

        return false;
    }

    @Override
    public Result handle(Request request) throws IOException {
        NetworkResponse response = downloader.load(request.uri);
        InputStream is = response.getInputStream();
        if (is != null) {
            return new Result(is, LoadedFrom.NETWORK);
        }
        return null;
    }
}
