package com.hanschen.easyloader.downloader;

import java.io.IOException;

/**
 * Created by Hans on 2016/8/19.
 */
public class ResponseException extends IOException {

    private static final long serialVersionUID = -8980140879491972755L;

    private final int responseCode;

    public ResponseException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
