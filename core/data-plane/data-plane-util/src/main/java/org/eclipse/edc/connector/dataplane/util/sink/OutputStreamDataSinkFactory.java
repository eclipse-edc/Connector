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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * A sink factory whose sole purpose is to validate {@link DataFlowRequest} whose destination address type is OutputStream.
 * This is required for synchronous data transfers in which an {@link OutputStreamDataSink} is used to convey the data to the call response.
 */
public class OutputStreamDataSinkFactory implements DataSinkFactory {

    public static final String TYPE = "OutputStream";
    private final Monitor monitor;
    private final ExecutorService executorService;

    public OutputStreamDataSinkFactory(Monitor monitor, ExecutorService executorService) {
        this.monitor = monitor;
        this.executorService = executorService;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        if (!canHandle(request)) {

            return Result.failure(String.format("%s: Cannot handle destination data address with type: %s",
                    getClass().getSimpleName(), request.getDestinationDataAddress().getType()));
        }

        return Result.success();
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        return new OutputStreamDataSink(request.getId(), executorService, monitor);
    }
}
