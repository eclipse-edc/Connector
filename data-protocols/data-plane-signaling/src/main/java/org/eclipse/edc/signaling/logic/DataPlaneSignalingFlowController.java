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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartedNotificationMessage;
import org.eclipse.edc.signaling.domain.DspDataAddress;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Data plane signaling implementation of the DataFlowController
 */
public class DataPlaneSignalingFlowController implements DataFlowController {

    private final ControlApiUrl callbackUrl;
    private final DataPlaneSelectorService selectorClient;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final ClientFactory clientFactory;
    private final DataAddressStore dataAddressStore;

    public DataPlaneSignalingFlowController(ControlApiUrl callbackUrl, DataPlaneSelectorService selectorClient,
                                            TypeTransformerRegistry typeTransformerRegistry, ClientFactory clientFactory,
                                            DataAddressStore dataAddressStore) {
        this.callbackUrl = callbackUrl;
        this.selectorClient = selectorClient;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.clientFactory = clientFactory;
        this.dataAddressStore = dataAddressStore;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return true;
    }

    @Override
    public StatusResult<DataFlowResponse> prepare(TransferProcess transferProcess, Policy policy) {
        var selection = selectorClient.selectFor(transferProcess);
        if (!selection.succeeded()) {
            return StatusResult.failure(FATAL_ERROR, selection.getFailureDetail());
        }

        var builder = DataFlowPrepareMessage.Builder.newInstance()
                .messageId(UUID.randomUUID().toString())
                .participantId(policy.getAssignee())
                .counterPartyId(policy.getAssigner())
                .dataspaceContext(transferProcess.getProtocol())
                .processId(transferProcess.getId())
                .agreementId(transferProcess.getContractId())
                .datasetId(transferProcess.getAssetId())
                .callbackAddress(callbackUrl.get())
                .transferType(transferProcess.getTransferType());

        var dataplaneMetadata = transferProcess.getDataplaneMetadata();
        if (dataplaneMetadata != null) {
            builder.labels(dataplaneMetadata.getLabels());
            builder.metadata(dataplaneMetadata.getProperties());
        }

        var message = builder.build();

        return clientFactory.createClient(selection.getContent())
                .prepare(message)
                .compose(response -> typeTransformerRegistry.transform(response, DataFlowResponse.class)
                        .flatMap(this::toStatusResult));
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        var selection = selectorClient.selectFor(transferProcess);
        if (!selection.succeeded()) {
            return StatusResult.failure(FATAL_ERROR, selection.getFailureDetail());
        }

        var builder = DataFlowStartMessage.Builder.newInstance()
                .messageId(UUID.randomUUID().toString())
                .participantId(policy.getAssignee())
                .counterPartyId(policy.getAssigner())
                .dataspaceContext(transferProcess.getProtocol())
                .processId(transferProcess.getId())
                .agreementId(transferProcess.getContractId())
                .datasetId(transferProcess.getAssetId())
                .callbackAddress(callbackUrl.get())
                .transferType(transferProcess.getTransferType());

        var dataAddress = dataAddressStore.resolve(transferProcess).orElse(f -> null);
        if (dataAddress != null) {
            var dspDataAddressTransformation = typeTransformerRegistry.transform(dataAddress, DspDataAddress.class);
            if (dspDataAddressTransformation.failed()) {
                return StatusResult.failure(FATAL_ERROR, dspDataAddressTransformation.getFailureDetail());
            }
            builder.dataAddress(dspDataAddressTransformation.getContent());
        }

        var dataplaneMetadata = transferProcess.getDataplaneMetadata();
        if (dataplaneMetadata != null) {
            builder.labels(dataplaneMetadata.getLabels());
            builder.metadata(dataplaneMetadata.getProperties());
        }

        var message = builder.build();

        return clientFactory.createClient(selection.getContent())
                .start(message)
                .compose(response -> typeTransformerRegistry.transform(response, DataFlowResponse.class)
                        .flatMap(this::toStatusResult));
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        var dataPlaneId = transferProcess.getDataPlaneId();
        if (dataPlaneId == null) {
            return StatusResult.fatalError("DataPlane id is null");
        }

        return selectorClient.findById(transferProcess.getDataPlaneId())
                .flatMap(this::toStatusResult)
                .map(clientFactory::createClient)
                .compose(client -> client.suspend(transferProcess.getId()));
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        var dataPlaneId = transferProcess.getDataPlaneId();
        if (dataPlaneId == null) {
            return StatusResult.success();
        }

        return selectorClient.findById(transferProcess.getDataPlaneId())
                .flatMap(this::toStatusResult)
                .map(clientFactory::createClient)
                .compose(client -> client.terminate(transferProcess.getId()));
    }

    @Override
    public StatusResult<Void> started(TransferProcess transferProcess) {
        var dataPlaneId = transferProcess.getDataPlaneId();
        if (dataPlaneId == null) {
            return StatusResult.fatalError("DataPlane id is null");
        }

        return selectorClient.findById(transferProcess.getDataPlaneId())
                .flatMap(this::toStatusResult)
                .map(clientFactory::createClient)
                .compose(client -> {
                    var builder = DataFlowStartedNotificationMessage.Builder.newInstance();
                    var dataAddress = transferProcess.getContentDataAddress();
                    if (dataAddress != null) {
                        var dspDataAddressTransformation = typeTransformerRegistry.transform(dataAddress, DspDataAddress.class);
                        if (dspDataAddressTransformation.failed()) {
                            return StatusResult.failure(FATAL_ERROR, dspDataAddressTransformation.getFailureDetail());
                        }
                        builder.dataAddress(dspDataAddressTransformation.getContent());
                    }

                    return client.started(transferProcess.getId(), builder.build());
                });
    }

    @Override
    public StatusResult<Void> completed(TransferProcess transferProcess) {
        var dataPlaneId = transferProcess.getDataPlaneId();
        if (dataPlaneId == null) {
            return StatusResult.fatalError("DataPlane id is null");
        }

        return selectorClient.findById(transferProcess.getDataPlaneId())
                .flatMap(this::toStatusResult)
                .map(clientFactory::createClient)
                .compose(client -> client.completed(transferProcess.getId()));
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return transferTypes();
    }

    @Override
    public Set<String> transferTypesFor(String assetId) {
        return transferTypes();
    }

    private @NotNull Set<String> transferTypes() {
        return selectorClient.getAll().map(Collection::stream)
                .map(it -> it.map(DataPlaneInstance::getAllowedTransferTypes)
                        .flatMap(Collection::stream).collect(toSet()))
                .orElse(f -> emptySet());
    }

    private @NotNull StatusResult<DataPlaneInstance> toStatusResult(ServiceResult<DataPlaneInstance> r) {
        if (r.succeeded()) {
            return StatusResult.success(r.getContent());
        } else {
            return StatusResult.failure(FATAL_ERROR, r.getFailureDetail());
        }
    }

    private @NotNull StatusResult<DataFlowResponse> toStatusResult(Result<DataFlowResponse> it) {
        if (it.succeeded()) {
            return StatusResult.success(it.getContent());
        } else {
            return StatusResult.failure(FATAL_ERROR, it.getFailureDetail());
        }
    }
}
