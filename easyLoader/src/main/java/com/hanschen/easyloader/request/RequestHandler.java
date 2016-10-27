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
package com.hanschen.easyloader.request;

import android.net.NetworkInfo;

import java.io.IOException;

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
     * @return 是否支持联网重试，若为true,则网络再次联通的时候将会重新请求
     */
    public boolean supportsReplay() {
        return false;
    }
}
