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
package site.hanschen.easyloader.request;

import android.support.annotation.NonNull;

/**
 * @author Hans.Chen
 */
public interface RequestTransformer {

    /**
     * Transform a request before it is submitted to be processed.
     *
     * @return The original request or a new request to replace it. Must not be null.
     */
    @NonNull
    Request transformRequest(Request request);

    /**
     * A {@link RequestTransformer} which returns the original request.
     */
    RequestTransformer IDENTITY = new RequestTransformer() {
        @NonNull
        @Override
        public Request transformRequest(Request request) {
            return request;
        }
    };
}
