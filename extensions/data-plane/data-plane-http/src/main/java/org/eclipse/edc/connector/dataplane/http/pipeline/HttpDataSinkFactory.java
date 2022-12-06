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

import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
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
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final int partitionSize;
    private final Monitor monitor;
    private final HttpRequestParamsSupplier supplier;

    public HttpDataSinkFactory(OkHttpClient httpClient,
                               ExecutorService executorService,
                               int partitionSize,
                               Monitor monitor,
                               HttpRequestParamsSupplier supplier) {
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
        this.monitor = monitor;
        this.supplier = supplier;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return HTTP_DATA.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        try {
            createSink(request);
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSink: " + e.getMessage());
        }
        return VALID;
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        return HttpDataSink.Builder.newInstance()
                .params(supplier.apply(request))
                .requestId(request.getId())
                .partitionSize(partitionSize)
                .httpClient(httpClient)
                .executorService(executorService)
                .monitor(monitor)
                .build();
    }
}
