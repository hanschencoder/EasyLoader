package com.hanschen.easyloader.log;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public interface Logger {

    void v(String tag, String msg);

    void d(String tag, String msg);

    void i(String tag, String msg);

    void w(String tag, String msg);

    void e(String tag, String msg);

    void e(String tag, String msg, Throwable t);

    void wtf(String tag, String msg);
}