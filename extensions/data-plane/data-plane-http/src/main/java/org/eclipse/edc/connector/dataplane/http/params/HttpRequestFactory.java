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
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;

/**
 * Permits to create a {@link Request} from a {@link HttpRequestParams}
 */
public class HttpRequestFactory {

    private static final String SLASH = "/";
    private static final String BACKSLASH = "\\";

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

        return toRequest(params, createRequestBody(params, bodySupplier, params.getContentType()));
    }

    /**
     * Creates HTTP request from the provided set of parameters and the request body supplier.
     *
     * @param params       the http request parameters.
     * @param part         the data source part.
     * @return HTTP request.
     */
    public Request toRequest(HttpRequestParams params, DataSource.Part part) {
        return toRequest(params, createRequestBody(params, part::openStream, part.mediaType()));
    }

    @NotNull
    private Request toRequest(HttpRequestParams params, RequestBody requestBody) {
        var requestBuilder = new Request.Builder()
                .url(toUrl(params))
                .method(params.getMethod(), requestBody);
        params.getHeaders().forEach(requestBuilder::addHeader);
        return requestBuilder.build();
    }

    @Nullable
    private RequestBody createRequestBody(HttpRequestParams params, @Nullable Supplier<InputStream> bodySupplier, String contentType) {
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
            var sanitizedPath = startWithSlash(path) ? path.substring(1) : path;
            builder.addPathSegments(sanitizedPath);
        }

        var queryParams = params.getQueryParams();
        if (!isNullOrBlank(queryParams)) {
            builder.query(queryParams);
        }
        return builder.build();
    }

    private static boolean startWithSlash(@NotNull String s) {
        return s.startsWith(SLASH) || s.startsWith(BACKSLASH);
    }
}
