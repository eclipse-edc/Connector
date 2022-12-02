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

package org.eclipse.edc.connector.dataplane.transfer.flow;

import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.HTTP_PROXY;

public class ProviderPushTransferDataFlowController implements DataFlowController {
    
    private final ControlPlaneApiUrl callbackUrl;
    private final DataPlaneClient dataPlaneClient;

    public ProviderPushTransferDataFlowController(ControlPlaneApiUrl callbackUrl, DataPlaneClient dataPlaneClient) {
        this.callbackUrl = callbackUrl;
        this.dataPlaneClient = dataPlaneClient;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return !HTTP_PROXY.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<Void> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        var dataFlowRequest = createRequest(dataRequest, contentAddress);
        var result = dataPlaneClient.transfer(dataFlowRequest);
        if (result.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to delegate data transfer to Data Plane: " + result.getFailureDetail());
        }
        return StatusResult.success();
    }

    private DataFlowRequest createRequest(DataRequest dataRequest, DataAddress sourceAddress) {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(dataRequest.getProcessId())
                .trackable(true)
                .sourceDataAddress(sourceAddress)
                .destinationType(dataRequest.getDestinationType())
                .destinationDataAddress(dataRequest.getDataDestination())
                .callbackAddress(callbackUrl != null ? callbackUrl.get() : null)
                .properties(dataRequest.getProperties())
                .build();
    }
}
