/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.http.params;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.http.pipeline.ChunkedTransferRequestBody;
import org.eclipse.edc.connector.dataplane.http.pipeline.NonChunkedTransferRequestBody;
import org.eclipse.edc.connector.dataplane.http.pipeline.StringRequestBodySupplier;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;

// TODO: document
public class HttpRequestFactory {

    /**
     * Creates HTTP request from the provided set of parameters.
     *
     * @param params the http request parameters
     * @return HTTP request.
     */
    public Request toRequest(HttpRequestParams params) {
        var bodySupplier = Optional.of(params)
                .map(HttpRequestParams::getBody)
                .map(StringRequestBodySupplier::new)
                .orElse(null);

        return toRequest(params, bodySupplier);
    }

    /**
     * Creates HTTP request from the provided set of parameters and the request body supplier.
     *
     * @param params the http request parameters
     * @param bodySupplier the request body supplier.
     * @return HTTP request.
     */
    public Request toRequest(HttpRequestParams params, Supplier<InputStream> bodySupplier) {
        var requestBody = createRequestBody(params, bodySupplier);
        var requestBuilder = new Request.Builder()
                .url(toUrl(params))
                .method(params.getMethod(), requestBody);
        params.getHeaders().forEach(requestBuilder::addHeader);
        return requestBuilder.build();
    }

    @Nullable
    private RequestBody createRequestBody(HttpRequestParams params, @Nullable Supplier<InputStream> bodySupplier) {
        var contentType = params.getContentType();
        if (bodySupplier == null || contentType == null) {
            return null;
        }
        return params.isNonChunkedTransfer()
                ? new NonChunkedTransferRequestBody(bodySupplier, contentType)
                : new ChunkedTransferRequestBody(bodySupplier, contentType);
    }

    /**
     * Creates a URL from the base url, path and query parameters provided in input.
     *
     * @return The URL.
     */
    private HttpUrl toUrl(HttpRequestParams params) {
        var baseUrl = params.getBaseUrl();
        var parsedBaseUrl = HttpUrl.parse(baseUrl);
        Objects.requireNonNull(parsedBaseUrl, "Failed to parse baseUrl: " + baseUrl);

        var builder = parsedBaseUrl.newBuilder();
        var path = params.getPath();
        if (!isNullOrBlank(path)) {
            builder.addPathSegments(path);
        }

        var queryParams = params.getQueryParams();
        if (!isNullOrBlank(queryParams)) {
            builder.query(queryParams);
        }
        return builder.build();
    }
}
