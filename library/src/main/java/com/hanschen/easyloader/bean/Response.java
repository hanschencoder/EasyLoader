package com.hanschen.easyloader.bean;

import java.io.InputStream;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public class Response {

    private final InputStream stream;
    private final long        contentLength;

    public Response(InputStream stream, long contentLength) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null.");
        }
        this.stream = stream;
        this.contentLength = contentLength;
    }

    public InputStream getInputStream() {
        return stream;
    }

    public long getContentLength() {
        return contentLength;
    }
}
