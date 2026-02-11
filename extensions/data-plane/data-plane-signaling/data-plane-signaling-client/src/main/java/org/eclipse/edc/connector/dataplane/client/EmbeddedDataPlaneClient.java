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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.client;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Objects;

/**
 * Implementation of a {@link DataPlaneClient} that uses a local {@link DataPlaneManager},
 * i.e. one that runs in the same JVM as the control plane.
 */
public class EmbeddedDataPlaneClient implements DataPlaneClient {

    private final DataPlaneManager dataPlaneManager;

    public EmbeddedDataPlaneClient(DataPlaneManager dataPlaneManager) {
        this.dataPlaneManager = Objects.requireNonNull(dataPlaneManager, "Data plane manager");
    }

    @Override
    public StatusResult<DataFlowResponseMessage> prepare(DataFlowProvisionMessage request) {
        var provisioning = dataPlaneManager.provision(request);
        if (provisioning.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, provisioning.getFailureDetail());
        }
        return StatusResult.success(provisioning.getContent());
    }

    @WithSpan
    @Override
    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage request) {
        var result = dataPlaneManager.validate(request);
        if (result.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, result.getFailureDetail());
        }
        var startResult = dataPlaneManager.start(request);
        if (startResult.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, startResult.getFailureDetail());
        }
        return StatusResult.success(startResult.getContent());
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        return dataPlaneManager.suspend(transferProcessId);
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        return dataPlaneManager.terminate(transferProcessId);
    }

}
