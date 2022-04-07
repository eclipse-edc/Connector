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

package org.eclipse.dataspaceconnector.transfer.core.flow;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult.failure;

/**
 * The default data flow manager.
 */
public class DataFlowManagerImpl implements DataFlowManager {
    private final List<DataFlowController> controllers = new ArrayList<>();

    @Override
    public void register(DataFlowController controller) {
        controllers.add(controller);
    }

    @WithSpan
    @Override
    public @NotNull DataFlowInitiateResult initiate(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        try {
            return controllers.stream()
                    .filter(controller -> controller.canHandle(dataRequest, contentAddress))
                    .findFirst()
                    .map(controller -> controller.initiateFlow(dataRequest, contentAddress, policy))
                    .orElseGet(() -> failure(FATAL_ERROR, controllerNotFound(dataRequest.getId())));
        } catch (Exception e) {
            return failure(FATAL_ERROR, runtimeException(dataRequest.getId(), e.getLocalizedMessage()));
        }
    }

    private String runtimeException(String id, String message) {
        return format("Unable to process data request %s. Data flow controller throws an exception: %s", id, message);
    }

    private String controllerNotFound(String id) {
        return format("Unable to process data request %s. No data flow controller found", id);
    }

}
