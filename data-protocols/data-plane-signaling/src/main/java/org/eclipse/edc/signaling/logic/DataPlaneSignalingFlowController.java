/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.logic;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Support Data Plane Signaling upcoming spec.
 */
public class DataPlaneSignalingFlowController implements DataFlowController {
    private final DataPlaneSelectorService dataPlaneSelectorService;
    private final TransferTypeParser transferTypeParser;

    public DataPlaneSignalingFlowController(DataPlaneSelectorService dataPlaneSelectorService,
                                            TransferTypeParser transferTypeParser) {
        this.dataPlaneSelectorService = dataPlaneSelectorService;
        this.transferTypeParser = transferTypeParser;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return transferTypeParser.parse(transferProcess.getTransferType()).succeeded();
    }

    @Override
    public StatusResult<DataFlowResponse> prepare(TransferProcess transferProcess, Policy policy) {
        return null;
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        return StatusResult.success(DataFlowResponse.Builder.newInstance().build());
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        return null;
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return null;
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return dataPlaneSelectorService.getAll()
                .map(dataPlaneInstances -> dataPlaneInstances.stream()
                        .map(DataPlaneInstance::getAllowedTransferTypes)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()))
                .orElse(f -> Set.of());
    }
}
