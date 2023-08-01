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

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link DataSource}s and optimized {@link PipelineService}s.
 */
public interface DataSourceFactory {

    /**
     * Returns true if this factory can create a {@link DataSource} for the request.
     */
    boolean canHandle(DataFlowRequest request);

    /**
     * Creates a source to access data to be sent.
     */
    DataSource createSource(DataFlowRequest request);

    /**
     * Returns a Result object of the validation result.
     */
    @NotNull Result<Void> validateRequest(DataFlowRequest request);

}
