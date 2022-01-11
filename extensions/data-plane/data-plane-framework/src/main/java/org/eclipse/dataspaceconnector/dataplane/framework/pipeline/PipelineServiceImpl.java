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
package org.eclipse.dataspaceconnector.dataplane.framework.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Default pipeline service implementation.
 */
public class PipelineServiceImpl implements PipelineService {
    private List<DataSourceFactory> sourceFactories = new ArrayList<>();
    private List<DataSinkFactory> sinkFactories = new ArrayList<>();

    @Override
    public CompletableFuture<Result<Void>> transfer(DataFlowRequest request) {
        var sourceFactory = getSourceFactory(request);
        var sinkFactory = getSinkFactory(request);
        var source = sourceFactory.createSource(request);
        var sink = sinkFactory.createSink(request);
        return sink.transfer(source);
    }

    @Override
    public void registerFactory(DataSourceFactory factory) {
        sourceFactories.add(factory);
    }

    @Override
    public void registerFactory(DataSinkFactory factory) {
        sinkFactories.add(factory);
    }

    @NotNull
    private DataSourceFactory getSourceFactory(DataFlowRequest request) {
        return sourceFactories.stream().filter(s -> s.canHandle(request)).findFirst()
                .orElseThrow(() -> new EdcException("Unknown data source type: " + request.getSourceDataAddress().getType()));
    }

    @NotNull
    private DataSinkFactory getSinkFactory(DataFlowRequest request) {
        return sinkFactories.stream().filter(s -> s.canHandle(request)).findFirst()
                .orElseThrow(() -> new EdcException("Unknown data sink type: " + request.getDestinationDataAddress().getType()));
    }

}
