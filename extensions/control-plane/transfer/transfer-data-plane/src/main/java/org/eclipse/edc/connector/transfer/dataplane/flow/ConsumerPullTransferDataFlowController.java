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

import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullTransferEndpointDataReferenceCreationRequest;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullTransferEndpointDataReferenceService;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataAddressConstants;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

public class ConsumerPullTransferDataFlowController implements DataFlowController {

    private final ConsumerPullTransferProxyResolver proxyResolver;
    private final ConsumerPullTransferEndpointDataReferenceService proxyReferenceService;

    public ConsumerPullTransferDataFlowController(ConsumerPullTransferProxyResolver proxyResolver, ConsumerPullTransferEndpointDataReferenceService proxyReferenceService) {
        this.proxyResolver = proxyResolver;
        this.proxyReferenceService = proxyReferenceService;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return HTTP_PROXY.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        var proxyUrl = proxyResolver.resolveProxyUrl(contentAddress);
        if (proxyUrl.failed()) {
            return StatusResult.failure(FATAL_ERROR, format("Failed to resolve proxy url for data request %s%n %s", dataRequest.getId(), proxyUrl.getFailureDetail()));
        }

        var proxyCreationRequest = ConsumerPullTransferEndpointDataReferenceCreationRequest.Builder.newInstance()
                .id(dataRequest.getId())
                .contentAddress(contentAddress)
                .proxyEndpoint(proxyUrl.getContent())
                .contractId(dataRequest.getContractId())
                .build();

        return proxyReferenceService.createProxyReference(proxyCreationRequest)
                .map(this::createResponse)
                .map(StatusResult::success)
                .orElse(failure -> StatusResult.failure(FATAL_ERROR, "Failed to generate proxy: " + failure.getFailureDetail()));
    }

    private DataFlowResponse createResponse(EndpointDataReference edr) {
        return DataFlowResponse.Builder.newInstance().dataAddress(EndpointDataAddressConstants.from(edr)).build();
    }
}
