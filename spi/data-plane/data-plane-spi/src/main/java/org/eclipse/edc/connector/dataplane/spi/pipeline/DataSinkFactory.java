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
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link DataFlowStartMessage}s
 */
public interface DataSinkFactory {

    /**
     * Return the supported DataAddress type.
     *
     * @return supported DataAddress type.
     */
    String supportedType();

    /**
     * Creates a sink to send data to.
     */
    DataSink createSink(DataFlowStartMessage request);

    /**
     * Returns a Result object of the validation result.
     */
    @NotNull Result<Void> validateRequest(DataFlowStartMessage request);

}
