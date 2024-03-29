/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.dataplane.flow;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toSet;

/**
 * Implementation of {@link DataFlowController} that is compliant with the data plane signaling.
 * <p>
 * It handles all the transfer process where the transferType met the criteria defined in the format mapping of the
 * signaling spec
 *
 * @see <a href="https://github.com/eclipse-edc/Connector/blob/main/docs/developer/data-plane-signaling/data-plane-signaling.md">Data plane signaling</a>
 * @see <a href="https://github.com/eclipse-edc/Connector/blob/main/docs/developer/data-plane-signaling/data-plane-signaling-mapping.md">Data plane signaling transfer type mapping</a>
 */
public class DataPlaneSignalingFlowController implements DataFlowController {

    private final ControlApiUrl callbackUrl;
    private final DataPlaneSelectorService selectorClient;
    private final DataPlaneClientFactory clientFactory;

    private final DataFlowPropertiesProvider propertiesProvider;
    private final String selectionStrategy;

    public DataPlaneSignalingFlowController(ControlApiUrl callbackUrl, DataPlaneSelectorService selectorClient, DataFlowPropertiesProvider propertiesProvider, DataPlaneClientFactory clientFactory, String selectionStrategy) {
        this.callbackUrl = callbackUrl;
        this.selectorClient = selectorClient;
        this.propertiesProvider = propertiesProvider;
        this.clientFactory = clientFactory;
        this.selectionStrategy = selectionStrategy;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return extractFlowType(transferProcess).succeeded();
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        var flowType = extractFlowType(transferProcess);
        if (flowType.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, flowType.getFailureDetail());
        }

        var propertiesResult = propertiesProvider.propertiesFor(transferProcess, policy);
        if (propertiesResult.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, propertiesResult.getFailureDetail());
        }

        var dataPlaneInstance = selectorClient.select(transferProcess.getContentDataAddress(), transferProcess.getDataDestination(), selectionStrategy, transferProcess.getTransferType());
        var dataFlowRequest = DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(transferProcess.getId())
                .sourceDataAddress(transferProcess.getContentDataAddress())
                .destinationDataAddress(transferProcess.getDataDestination())
                .participantId(policy.getAssignee())
                .agreementId(transferProcess.getContractId())
                .assetId(transferProcess.getAssetId())
                .flowType(flowType.getContent())
                .callbackAddress(callbackUrl != null ? callbackUrl.get() : null)
                .properties(propertiesResult.getContent())
                .build();

        var dataPlaneInstanceId = dataPlaneInstance != null ? dataPlaneInstance.getId() : null;

        return clientFactory.createClient(dataPlaneInstance)
                .start(dataFlowRequest)
                .map(it -> DataFlowResponse.Builder.newInstance()
                        .dataAddress(it.getDataAddress())
                        .dataPlaneId(dataPlaneInstanceId)
                        .build()
                );
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        return selectorClient.getAll().stream()
                .filter(dataPlaneInstanceFilter(transferProcess))
                .map(clientFactory::createClient)
                .map(client -> client.suspend(transferProcess.getId()))
                .reduce(StatusResult::merge)
                .orElse(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to select the data plane for suspending the transfer process %s".formatted(transferProcess.getId())));
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return selectorClient.getAll().stream()
                .filter(dataPlaneInstanceFilter(transferProcess))
                .map(clientFactory::createClient)
                .map(client -> client.terminate(transferProcess.getId()))
                .reduce(StatusResult::merge)
                .orElse(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to select the data plane for terminating the transfer process %s".formatted(transferProcess.getId())));
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return selectorClient.getAll().stream()
                .filter(it -> it.getAllowedSourceTypes().contains(asset.getDataAddress().getType()))
                .map(DataPlaneInstance::getAllowedTransferTypes)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private StatusResult<FlowType> extractFlowType(TransferProcess transferProcess) {
        return Optional.ofNullable(transferProcess.getTransferType())
                .map(transferType -> transferType.split("-"))
                .filter(tokens -> tokens.length == 2)
                .map(tokens -> parseFlowType(tokens[1]))
                .orElse(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to extract flow type from transferType %s".formatted(transferProcess.getTransferType())));

    }

    private StatusResult<FlowType> parseFlowType(String flowType) {
        try {
            return StatusResult.success(FlowType.valueOf(flowType));
        } catch (Exception e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Unknown flow type %s".formatted(flowType));
        }
    }

    private Predicate<DataPlaneInstance> dataPlaneInstanceFilter(TransferProcess transferProcess) {
        if (transferProcess.getDataPlaneId() != null) {
            return dataPlaneInstance -> dataPlaneInstance.getId().equals(transferProcess.getDataPlaneId());
        } else {
            return d -> true;
        }
    }
}
