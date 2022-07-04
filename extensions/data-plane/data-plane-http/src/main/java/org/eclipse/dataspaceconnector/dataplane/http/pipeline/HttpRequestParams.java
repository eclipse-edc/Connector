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

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okhttp3.Request;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class HttpRequestParams {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private String method;
    private String baseUrl;
    private String path;
    private String queryParams;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private String body;
    private final Map<String, String> headers = new HashMap<>();

    /**
     * Creates HTTP request from the current set of parameters.
     *
     * @return HTTP request.
     */
    public Request toRequest() {
        if (body == null) {
            return toRequest(null);
        }
        return toRequest(new StringRequestBodySupplier(body));
    }

    /**
     * Creates HTTP request from the current set of parameters and the provided request body.
     *
     * @param bodySupplier the request body supplier.
     * @return HTTP request.
     */
    public Request toRequest(@Nullable Supplier<InputStream> bodySupplier) {
        var requestBody = (bodySupplier != null && contentType != null) ?
                new StreamingRequestBody(bodySupplier, contentType) :
                null;

        var requestBuilder = new Request.Builder()
                .url(toUrl())
                .method(method, requestBody);
        headers.forEach(requestBuilder::addHeader);
        return requestBuilder.build();
    }

    /**
     * Creates a URL from the base url, path and query parameters provided in input.
     *
     * @return The URL.
     */
    private String toUrl() {
        var url = baseUrl;
        if (path != null && !path.isBlank()) {
            url += "/" + path;
        }
        if (queryParams != null && !queryParams.isBlank()) {
            url += "?" + queryParams;
        }
        return url;
    }

    public static class Builder {
        private final HttpRequestParams params;

        public static HttpRequestParams.Builder newInstance() {
            return new HttpRequestParams.Builder();
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

        public HttpRequestParams build() {
            params.headers.forEach((s, s2) -> Objects.requireNonNull(s2, "value for header: " + s));
            Objects.requireNonNull(params.baseUrl, "baseUrl");
            Objects.requireNonNull(params.method, "method");
            Objects.requireNonNull(params.contentType, "contentType");
            params.headers.forEach((s, s2) -> Objects.requireNonNull(s2, "value for header: " + s));
            return params;
        }

        private Builder() {
            params = new HttpRequestParams();
        }
    }
}
