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
package com.hanschen.easyloader;

import android.graphics.Color;

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
