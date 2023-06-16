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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.flow;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyResolver;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerPullTransferDataFlowControllerTest {

    private final DataPlaneSelectorClient selectorClient = mock(DataPlaneSelectorClient.class);
    private final ConsumerPullDataPlaneProxyResolver resolver = mock(ConsumerPullDataPlaneProxyResolver.class);

    private final ConsumerPullTransferDataFlowController flowController = new ConsumerPullTransferDataFlowController(selectorClient, resolver);

    @Test
    void verifyCanHandle() {
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType(HTTP_PROXY).build(), null)).isTrue();
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType("not-http-proxy").build(), null)).isFalse();
    }

    @Test
    void verifyInitiateFlowSuccess() {
        var request = dataRequest();
        var proxyAddress = dataAddress();
        var contentAddress = dataAddress();
        var instance = mock(DataPlaneInstance.class);

        when(selectorClient.find(eq(contentAddress), argThat(destination -> destination.getType().equals(HTTP_PROXY)))).thenReturn(instance);
        when(resolver.toDataAddress(request, contentAddress, instance)).thenReturn(Result.success(proxyAddress));

        var result = flowController.initiateFlow(request, contentAddress, null);

        assertThat(result.succeeded()).isTrue();
        var response = result.getContent();
        assertThat(response.getDataAddress()).isEqualTo(proxyAddress);
    }

    @Test
    void verifyInitiateFlowReturnsFailureIfNoDataPlaneInstance() {
        var request = dataRequest();
        var contentAddress = dataAddress();

        var result = flowController.initiateFlow(request, contentAddress, null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail())
                .isEqualTo(String.format("Failed to find DataPlaneInstance for source/destination: %s/%s", contentAddress.getType(), HTTP_PROXY));
    }

    @Test
    void verifyInitiateFlowReturnsFailureIfAddressResolutionFails() {
        var request = dataRequest();
        var contentAddress = dataAddress();
        var errorMsg = "Test Error Message";
        var instance = mock(DataPlaneInstance.class);

        when(selectorClient.find(eq(contentAddress), argThat(destination -> destination.getType().equals(HTTP_PROXY)))).thenReturn(instance);
        when(resolver.toDataAddress(request, contentAddress, instance)).thenReturn(Result.failure(errorMsg));

        var result = flowController.initiateFlow(request, contentAddress, Policy.Builder.newInstance().build());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains(errorMsg);
    }

    private DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
    }

    private DataRequest dataRequest() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .connectorAddress("test.connector.address")
                .processId(UUID.randomUUID().toString())
                .destinationType(HTTP_PROXY)
                .build();
    }
}
