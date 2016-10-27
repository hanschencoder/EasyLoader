package com.hanschen.easyloader.request;

import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.bean.NetworkResponse;
import com.hanschen.easyloader.downloader.Downloader;

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
