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
import org.eclipse.edc.connector.controlplane.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.FlowTypeExtractor;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static java.util.Collections.emptySet;
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
    private final FlowTypeExtractor flowTypeExtractor;

    public DataPlaneSignalingFlowController(ControlApiUrl callbackUrl, DataPlaneSelectorService selectorClient,
                                            DataFlowPropertiesProvider propertiesProvider, DataPlaneClientFactory clientFactory,
                                            String selectionStrategy, FlowTypeExtractor flowTypeExtractor) {
        this.callbackUrl = callbackUrl;
        this.selectorClient = selectorClient;
        this.propertiesProvider = propertiesProvider;
        this.clientFactory = clientFactory;
        this.selectionStrategy = selectionStrategy;
        this.flowTypeExtractor = flowTypeExtractor;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return flowTypeExtractor.extract(transferProcess.getTransferType()).succeeded();
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        var flowType = flowTypeExtractor.extract(transferProcess.getTransferType());
        if (flowType.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, flowType.getFailureDetail());
        }

        var propertiesResult = propertiesProvider.propertiesFor(transferProcess, policy);
        if (propertiesResult.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, propertiesResult.getFailureDetail());
        }

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

        var selection = selectorClient.select(transferProcess.getContentDataAddress(), transferProcess.getTransferType(), selectionStrategy);
        if (selection.succeeded()) {
            var dataPlaneInstance = selection.getContent();
            return clientFactory.createClient(dataPlaneInstance)
                    .start(dataFlowRequest)
                    .map(it -> DataFlowResponse.Builder.newInstance()
                            .dataAddress(it.getDataAddress())
                            .dataPlaneId(dataPlaneInstance.getId())
                            .build()
                    );
        } else {
            // TODO: this branch works for embedded data plane but it is a potential false positive when the dataplane is not found, needs to be refactored
            return clientFactory.createClient(null)
                    .start(dataFlowRequest)
                    .map(it -> DataFlowResponse.Builder.newInstance()
                            .dataAddress(it.getDataAddress())
                            .dataPlaneId(null)
                            .build()
                    );
        }
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        return onDataplaneInstancesDo("suspending", transferProcess, DataPlaneClient::suspend);
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return onDataplaneInstancesDo("terminating", transferProcess, DataPlaneClient::terminate);
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        var result = selectorClient.getAll();
        if (result.failed()) {
            return emptySet();
        }

        return result.getContent().stream()
                .filter(it -> it.getAllowedSourceTypes().contains(asset.getDataAddress().getType()))
                .map(DataPlaneInstance::getAllowedTransferTypes)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private StatusResult<Void> onDataplaneInstancesDo(String name, TransferProcess transferProcess, BiFunction<DataPlaneClient, String, StatusResult<Void>> action) {
        var result = selectorClient.getAll();
        if (result.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, result.getFailureDetail());
        }

        return result.getContent().stream()
                .filter(dataPlaneInstanceFilter(transferProcess))
                .map(clientFactory::createClient)
                .map(client -> action.apply(client, transferProcess.getId()))
                .reduce(StatusResult::merge)
                .orElse(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to select the data plane for %s the transfer process %s".formatted(name, transferProcess.getId())));
    }

    private Predicate<DataPlaneInstance> dataPlaneInstanceFilter(TransferProcess transferProcess) {
        if (transferProcess.getDataPlaneId() != null) {
            return dataPlaneInstance -> dataPlaneInstance.getId().equals(transferProcess.getDataPlaneId());
        } else {
            return d -> true;
        }
    }
}
