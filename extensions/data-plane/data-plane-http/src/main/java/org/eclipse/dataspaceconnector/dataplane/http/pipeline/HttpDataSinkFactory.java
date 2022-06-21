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

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress.DATA_TYPE;

/**
 * Instantiates {@link HttpDataSink}s for requests whose source data type is {@link HttpDataAddress#DATA_TYPE}.
 */
public class HttpDataSinkFactory implements DataSinkFactory {
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final int partitionSize;
    private final Monitor monitor;

    public HttpDataSinkFactory(OkHttpClient httpClient, ExecutorService executorService, int partitionSize, Monitor monitor) {
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return DATA_TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var result = createDataSink(request);
        return result.succeeded() ? VALID : Result.failure(result.getFailureMessages());
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var result = createDataSink(request);
        if (result.failed()) {
            throw new EdcException("Failed to create sink: " + String.join(",", result.getFailureMessages()));
        }
        return result.getContent();
    }

    private Result<DataSink> createDataSink(DataFlowRequest request) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .copyFrom(request.getDestinationDataAddress())
                .build();
        var requestId = request.getId();
        var baseUrl = dataAddress.getBaseUrl();
        if (baseUrl == null) {
            return Result.failure("Missing mandatory base url for request: " + requestId);
        }
        var authKey = dataAddress.getAuthKey();
        var authCode = dataAddress.getAuthCode();

        var sink = HttpDataSink.Builder.newInstance()
                .endpoint(baseUrl)
                .requestId(requestId)
                .partitionSize(partitionSize)
                .authKey(authKey)
                .authCode(authCode)
                .httpClient(httpClient)
                .executorService(executorService)
                .monitor(monitor)
                .build();

        return Result.success(sink);
    }
}
