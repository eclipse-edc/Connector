/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.transfer.flow;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * The default data flow manager.
 */
public class DataFlowManagerImpl implements DataFlowManager {

    private final List<PrioritizedDataFlowController> controllers = new ArrayList<>();

    @Override
    public void register(DataFlowController controller) {
        controllers.add(new PrioritizedDataFlowController(0, controller));
    }

    @Override
    public void register(int priority, DataFlowController controller) {
        controllers.add(new PrioritizedDataFlowController(priority, controller));
    }

    @WithSpan
    @Override
    public @NotNull StatusResult<DataFlowResponse> initiate(TransferProcess transferProcess, Policy policy) {
        try {
            return controllers.stream()
                    .sorted(Comparator.comparingInt(a -> -a.priority))
                    .map(PrioritizedDataFlowController::controller)
                    .filter(controller -> controller.canHandle(transferProcess))
                    .findFirst()
                    .map(controller -> controller.initiateFlow(transferProcess, policy))
                    .orElseGet(() -> StatusResult.failure(FATAL_ERROR, controllerNotFound(transferProcess.getId())));
        } catch (Exception e) {
            return StatusResult.failure(FATAL_ERROR, runtimeException(transferProcess.getId(), e.getLocalizedMessage()));
        }
    }

    private String runtimeException(String id, String message) {
        return format("Unable to process transfer %s. Data flow controller throws an exception: %s", id, message);
    }

    private String controllerNotFound(String id) {
        return format("Unable to process transfer %s. No data flow controller found", id);
    }

    record PrioritizedDataFlowController(int priority, DataFlowController controller) { }

}
