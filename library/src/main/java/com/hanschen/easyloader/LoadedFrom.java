package com.hanschen.easyloader;

import android.graphics.Color;

/**
 * Created by Hans on 2016/8/19.
 */
public enum LoadedFrom {

    MEMORY(Color.GREEN, "MEMORY"),

    DISK(Color.BLUE, "DISK"),

    NETWORK(Color.RED, "NETWORK");

    final int    debugColor;
    final String description;

    LoadedFrom(int debugColor, String description) {
        this.debugColor = debugColor;
        this.description = description;
    }

    @Override
    public String toString() {
        return "LoadedFrom{" +
                "description='" + description + '\'' +
                '}';
    }
}
