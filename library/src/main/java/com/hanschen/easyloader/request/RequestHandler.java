package com.hanschen.easyloader.request;

import java.io.IOException;

/**
 * Created by Hans on 2016/8/19.
 */
public interface RequestHandler {

    boolean canHandleRequest(Request request);

    Result handle(Request request) throws IOException;
}
