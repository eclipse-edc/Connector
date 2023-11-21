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

package org.eclipse.edc.connector.transfer.dataplane.flow;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;

public class ProviderPushTransferDataFlowController implements DataFlowController {

    private final ControlApiUrl callbackUrl;
    private final DataPlaneSelectorClient selectorClient;
    private final DataPlaneClientFactory clientFactory;

    public ProviderPushTransferDataFlowController(ControlApiUrl callbackUrl, DataPlaneSelectorClient selectorClient, DataPlaneClientFactory clientFactory) {
        this.callbackUrl = callbackUrl;
        this.selectorClient = selectorClient;
        this.clientFactory = clientFactory;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return !HTTP_PROXY.equals(transferProcess.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> initiateFlow(TransferProcess transferProcess, Policy policy) {
        var dataFlowRequest = DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(transferProcess.getId())
                .trackable(true)
                .sourceDataAddress(transferProcess.getContentDataAddress())
                .destinationDataAddress(transferProcess.getDataDestination())
                .callbackAddress(callbackUrl != null ? callbackUrl.get() : null)
                .build();

        var dataPlaneInstance = selectorClient.find(transferProcess.getContentDataAddress(), transferProcess.getDataDestination());
        return clientFactory.createClient(dataPlaneInstance)
                .transfer(dataFlowRequest)
                .map(it -> DataFlowResponse.Builder.newInstance().build());
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return selectorClient.getAll().stream().map(clientFactory::createClient)
                .map(client -> client.terminate(transferProcess.getId()))
                .reduce(StatusResult::merge)
                .orElse(StatusResult.success());
    }

}
