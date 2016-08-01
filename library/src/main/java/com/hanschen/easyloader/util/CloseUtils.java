package com.hanschen.easyloader.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by chenhang on 2016/6/1.
 */
public class CloseUtils {

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
}
