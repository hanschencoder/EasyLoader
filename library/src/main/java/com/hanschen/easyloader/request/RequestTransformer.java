package com.hanschen.easyloader.request;

/**
 * @author Hans.Chen
 */
public interface RequestTransformer {

    /**
     * Transform a request before it is submitted to be processed.
     *
     * @return The original request or a new request to replace it. Must not be null.
     */
    Request transformRequest(Request request);

    /**
     * A {@link RequestTransformer} which returns the original request.
     */
    RequestTransformer IDENTITY = new RequestTransformer() {
        @Override
        public Request transformRequest(Request request) {
            return request;
        }
    };
}
