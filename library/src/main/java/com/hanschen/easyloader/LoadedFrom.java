package com.hanschen.easyloader;

import android.graphics.Color;

/**
 * Created by Hans on 2016/8/19.
 */
public enum LoadedFrom {

    MEMORY(Color.GREEN),

    DISK(Color.BLUE),

    NETWORK(Color.RED);

    final int debugColor;

    LoadedFrom(int debugColor) {
        this.debugColor = debugColor;
    }
}
