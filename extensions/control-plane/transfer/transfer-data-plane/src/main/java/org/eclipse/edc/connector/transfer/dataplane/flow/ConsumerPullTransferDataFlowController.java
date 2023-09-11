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

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyResolver;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.response.StatusResult.failure;

public class ConsumerPullTransferDataFlowController implements DataFlowController {

    private final DataPlaneSelectorClient selectorClient;
    private final ConsumerPullDataPlaneProxyResolver resolver;

    public ConsumerPullTransferDataFlowController(DataPlaneSelectorClient selectorClient, ConsumerPullDataPlaneProxyResolver resolver) {
        this.selectorClient = selectorClient;
        this.resolver = resolver;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return HTTP_PROXY.equals(transferProcess.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> initiateFlow(TransferProcess transferProcess, Policy policy) {
        var contentAddress = transferProcess.getContentDataAddress();
        var dataRequest = transferProcess.getDataRequest();
        return Optional.ofNullable(selectorClient.find(contentAddress, dataRequest.getDataDestination()))
                .map(instance -> resolver.toDataAddress(dataRequest, contentAddress, instance)
                        .map(this::toResponse)
                        .map(StatusResult::success)
                        .orElse(failure -> failure(FATAL_ERROR, "Failed to generate proxy: " + failure.getFailureDetail())))
                .orElse(failure(FATAL_ERROR, format("Failed to find DataPlaneInstance for source/destination: %s/%s", contentAddress.getType(), HTTP_PROXY)));
    }

    private DataFlowResponse toResponse(DataAddress address) {
        return DataFlowResponse.Builder.newInstance().dataAddress(address).build();
    }
}
