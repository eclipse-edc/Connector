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

package org.eclipse.edc.connector.dataplane.spi.pipeline;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Transfers data from a source to a sink.
 */
@ExtensionPoint
public interface PipelineService extends TransferService {

    /**
     * Transfers data using the supplied data sink.
     */
    CompletableFuture<StreamResult<Void>> transfer(DataSink sink, DataFlowRequest request);

    /**
     * Registers a factory for creating data sources.
     */
    void registerFactory(DataSourceFactory factory);

    /**
     * Registers a factory for creating data sinks.
     */
    void registerFactory(DataSinkFactory factory);
}
