/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.NAME;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.TYPE;

/**
 * Instantiates {@link HttpDataSource}s for requests whose source data type is {@link HttpDataSchema#TYPE}.
 */
public class HttpDataSourceFactory implements DataSourceFactory {
    private final OkHttpClient httpClient;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;

    public HttpDataSourceFactory(OkHttpClient httpClient, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var result = createDataSource(request);
        return result.succeeded() ? VALID : Result.failure(result.getFailureMessages());
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var result = createDataSource(request);
        if (result.failed()) {
            throw new EdcException("Failed to create source: " + String.join(",", result.getFailureMessages()));
        }
        return createDataSource(request).getContent();
    }

    private Result<HttpDataSource> createDataSource(DataFlowRequest request) {
        var dataAddress = request.getSourceDataAddress();
        var endpoint = dataAddress.getProperty(ENDPOINT);
        if (StringUtils.isNullOrBlank(endpoint)) {
            return Result.failure("HTTP data source endpoint not provided for request: " + request.getId());
        }
        var method = request.getProperties().get(METHOD);
        if (StringUtils.isNullOrBlank(method)) {
            return Result.failure("Method not provided for request: " + request.getId());
        }

        var name = dataAddress.getProperty(NAME);

        var mediaTypeStr = request.getProperties().get(MEDIA_TYPE);
        MediaType mediaType = null;
        if (mediaTypeStr != null) {
            mediaType = MediaType.parse(mediaTypeStr);
            if (mediaType == null) {
                return Result.failure("Unhandled media type: " + mediaTypeStr);
            }
        }

        var builder = HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .requestId(request.getId())
                .sourceUrl(endpoint)
                .name(name)
                .method(method)
                .retryPolicy(retryPolicy)
                .monitor(monitor);
        Optional.ofNullable(mediaType).ifPresent(mt -> builder.requestBody(mt, request.getProperties().get(BODY)));
        Optional.ofNullable(dataAddress.getProperty(AUTHENTICATION_KEY))
                .ifPresent(s -> builder.header(s, dataAddress.getProperty(AUTHENTICATION_CODE)));
        Optional.ofNullable(request.getProperties().get(QUERY_PARAMS))
                .ifPresent(builder::queryParams);

        try {
            return Result.success(builder.build());
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSource: " + e.getMessage());
        }
    }
}
