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
import org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.HTTP_DATA_TYPE;

/**
 * Instantiates {@link HttpDataSink}s for requests whose source data type is {@link HttpDataAddressSchema#HTTP_DATA_TYPE}.
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
    public String supportedType() {
        return HTTP_DATA_TYPE;
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        try {
            createSink(request);
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSink: " + e.getMessage());
        }
        return Result.success();
    }

    @Override
    public DataSink createSink(DataFlowStartMessage request) {
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
