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

import java.util.Set;

/**
 * Transfers data from a {@link DataSource} to a {@link DataSink} using Data Plane extensions.
 * Represent the default {@link TransferService} that leverages on the internal Data-Plane transfer mechanism.
 */
@ExtensionPoint
public interface PipelineService extends TransferService {

    /**
     * Registers a factory for creating data sources.
     */
    void registerFactory(DataSourceFactory factory);

    /**
     * Registers a factory for creating data sinks.
     */
    void registerFactory(DataSinkFactory factory);

    /**
     * Return a collection of source DataAddress types supported.
     *
     * @return set of types.
     */
    Set<String> supportedSourceTypes();

    /**
     * Return a collection of sink DataAddress types supported.
     *
     * @return set of types.
     */
    Set<String> supportedSinkTypes();

}
