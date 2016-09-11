package com.hanschen.easyloader.request;

import android.net.NetworkInfo;

import java.io.IOException;

/**
 * Created by Hans on 2016/8/19.
 */
public abstract class RequestHandler {

    /**
     * @return 当前RequestHandler是否可以处理这个request
     */
    public abstract boolean canHandleRequest(Request request);

    /**
     * 处理request，返回结果
     */
    public abstract Result handle(Request request) throws IOException;

    /**
     * @return 重试最大次数
     */
    public int getRetryCount() {
        return 0;
    }

    /**
     * @return 根据当前环境判断是否应重试
     */
    public boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
        return false;
    }

    /**
     * @return 是否放入重新请求列表，若为true,则网络再次联通的时候将会重新请求
     */
    public boolean supportsReplay() {
        return false;
    }
}
