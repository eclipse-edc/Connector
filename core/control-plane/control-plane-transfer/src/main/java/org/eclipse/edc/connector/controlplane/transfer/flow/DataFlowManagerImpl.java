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

package org.eclipse.edc.connector.controlplane.transfer.flow;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * The default data flow manager.
 */
public class DataFlowManagerImpl implements DataFlowManager {

    private final List<PrioritizedDataFlowController> controllers = new ArrayList<>();
    private final Monitor monitor;
    private final AssetIndex assetIndex;

    public DataFlowManagerImpl(Monitor monitor, AssetIndex assetIndex) {
        this.monitor = monitor;
        this.assetIndex = assetIndex;
    }

    @Override
    public void register(DataFlowController controller) {
        controllers.add(new PrioritizedDataFlowController(0, controller));
    }

    @Override
    public void register(int priority, DataFlowController controller) {
        controllers.add(new PrioritizedDataFlowController(priority, controller));
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> prepare(TransferProcess transferProcess, Policy policy) {
        try {
            return chooseController(transferProcess)
                    .map(controller -> controller.prepare(transferProcess, policy))
                    .orElseGet(() -> StatusResult.failure(FATAL_ERROR, controllerNotFound(transferProcess.getId())));
        } catch (Exception e) {
            var message = runtimeException(transferProcess.getId(), e.getMessage());
            monitor.severe(message, e);
            return StatusResult.failure(FATAL_ERROR, message);
        }
    }

    @WithSpan
    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        try {
            return chooseController(transferProcess)
                    .map(controller -> controller.start(transferProcess, policy))
                    .orElseGet(() -> StatusResult.failure(FATAL_ERROR, controllerNotFound(transferProcess.getId())));
        } catch (Exception e) {
            var message = runtimeException(transferProcess.getId(), e.getMessage());
            monitor.severe(message, e);
            return StatusResult.failure(FATAL_ERROR, message);
        }
    }

    @Override
    public @NotNull StatusResult<Void> terminate(TransferProcess transferProcess) {
        return chooseController(transferProcess)
                .map(controller -> controller.terminate(transferProcess))
                .orElseGet(() -> StatusResult.failure(FATAL_ERROR, controllerNotFound(transferProcess.getId())));
    }

    @Override
    public @NotNull StatusResult<Void> suspend(TransferProcess transferProcess) {
        return chooseController(transferProcess)
                .map(controller -> controller.suspend(transferProcess))
                .orElseGet(() -> StatusResult.failure(FATAL_ERROR, controllerNotFound(transferProcess.getId())));
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return controllers.stream()
                .map(it -> it.controller)
                .map(it -> it.transferTypesFor(asset))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    @Override
    public Set<String> transferTypesFor(String assetId) {
        return Optional.ofNullable(assetIndex.findById(assetId))
                .map(this::transferTypesFor)
                .orElseGet(Collections::emptySet);
    }

    private Optional<DataFlowController> chooseController(TransferProcess transferProcess) {
        return controllers.stream()
                .sorted(Comparator.comparingInt(a -> -a.priority))
                .map(PrioritizedDataFlowController::controller)
                .filter(controller -> controller.canHandle(transferProcess))
                .findFirst();
    }

    private String runtimeException(String id, String message) {
        return format("Unable to process transfer %s. Data flow controller throws an exception: %s", id, message);
    }

    private String controllerNotFound(String id) {
        return format("Unable to process transfer %s. No data flow controller found", id);
    }

    record PrioritizedDataFlowController(int priority, DataFlowController controller) {
    }

}
