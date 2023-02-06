/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.http.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpRequestParams {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final boolean DEFAULT_NON_CHUNKED_TRANSFER = false;

    private String method;
    private String baseUrl;
    private String path;
    private String queryParams;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private String body;
    private boolean nonChunkedTransfer = DEFAULT_NON_CHUNKED_TRANSFER;
    private final Map<String, String> headers = new HashMap<>();

    public String getMethod() {
        return method;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPath() {
        return path;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }

    public boolean isNonChunkedTransfer() {
        return nonChunkedTransfer;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static class Builder {
        private final HttpRequestParams params;

        public static HttpRequestParams.Builder newInstance() {
            return new HttpRequestParams.Builder();
        }

        private Builder() {
            params = new HttpRequestParams();
        }

        public HttpRequestParams.Builder baseUrl(String baseUrl) {
            params.baseUrl = baseUrl;
            return this;
        }

        public HttpRequestParams.Builder queryParams(String queryParams) {
            params.queryParams = queryParams;
            return this;
        }

        public HttpRequestParams.Builder method(String method) {
            params.method = method;
            return this;
        }

        public HttpRequestParams.Builder header(String key, String value) {
            params.headers.put(key, value);
            return this;
        }

        public HttpRequestParams.Builder headers(Map<String, String> headers) {
            params.headers.putAll(headers);
            return this;
        }

        public HttpRequestParams.Builder contentType(String contentType) {
            params.contentType = contentType;
            return this;
        }

        public HttpRequestParams.Builder body(String body) {
            params.body = body;
            return this;
        }

        public HttpRequestParams.Builder path(String path) {
            params.path = path;
            return this;
        }

        public HttpRequestParams.Builder nonChunkedTransfer(boolean nonChunkedTransfer) {
            params.nonChunkedTransfer = nonChunkedTransfer;
            return this;
        }

        public HttpRequestParams build() {
            params.headers.forEach((s, s2) -> Objects.requireNonNull(s2, "value for header: " + s));
            Objects.requireNonNull(params.baseUrl, "baseUrl");
            Objects.requireNonNull(params.method, "method");
            Objects.requireNonNull(params.contentType, "contentType");
            params.headers.forEach((s, s2) -> Objects.requireNonNull(s2, "value for header: " + s));
            return params;
        }
    }
}
