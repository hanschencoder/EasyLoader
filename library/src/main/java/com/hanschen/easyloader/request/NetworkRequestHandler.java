package com.hanschen.easyloader.request;

import com.hanschen.easyloader.LoadedFrom;
import com.hanschen.easyloader.bean.Response;
import com.hanschen.easyloader.downloader.Downloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Hans on 2016/8/19.
 */
public class NetworkRequestHandler implements RequestHandler {

    private static final String SCHEME_HTTP  = "http";
    private static final String SCHEME_HTTPS = "https";

    private final Downloader downloader;

    public NetworkRequestHandler(Downloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean canHandleRequest(Request request) {
        String scheme = request.uri.getScheme();
        return (SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme));
    }

    @Override
    public Result handle(Request request) throws IOException {
        Response response = downloader.load(request.uri);
        InputStream is = response.getInputStream();
        if (is != null) {
            return new Result(is, LoadedFrom.NETWORK);
        }
        return null;
    }
}
