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
 *       Siemens AG - added additionalHeaders
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.edc.spi.types.domain.HttpDataAddress.HTTP_DATA;

/**
 * Instantiates {@link HttpDataSink}s for requests whose source data type is {@link HttpDataAddress#HTTP_DATA}.
 */
public class HttpDataSinkFactory implements DataSinkFactory {
    private final EdcHttpClient httpClient;
    private final ExecutorService executorService;
    private final int partitionSize;
    private final Monitor monitor;
    private final HttpRequestParamsProvider requestParamsProvider;
    private final HttpRequestFactory requestFactory;

    public HttpDataSinkFactory(EdcHttpClient httpClient,
                               ExecutorService executorService,
                               int partitionSize,
                               Monitor monitor,
                               HttpRequestParamsProvider requestParamsProvider, HttpRequestFactory requestFactory) {
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
        this.monitor = monitor;
        this.requestParamsProvider = requestParamsProvider;
        this.requestFactory = requestFactory;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return HTTP_DATA.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        return validateRequest(request).map(it -> true);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        try {
            createSink(request);
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSink: " + e.getMessage());
        }
        return Result.success();
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        return HttpDataSink.Builder.newInstance()
                .params(requestParamsProvider.provideSinkParams(request))
                .requestId(request.getId())
                .partitionSize(partitionSize)
                .httpClient(httpClient)
                .executorService(executorService)
                .monitor(monitor)
                .requestFactory(requestFactory)
                .build();
    }
}
