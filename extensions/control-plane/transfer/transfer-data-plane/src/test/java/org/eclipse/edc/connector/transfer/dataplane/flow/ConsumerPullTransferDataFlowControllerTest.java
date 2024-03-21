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

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyResolver;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerPullTransferDataFlowControllerTest {

    private final DataPlaneSelectorService selectorService = mock();
    private final ConsumerPullDataPlaneProxyResolver resolver = mock();

    private final ConsumerPullTransferDataFlowController flowController = new ConsumerPullTransferDataFlowController(selectorService, resolver);

    private static final String HTTP_DATA_PULL = "HttpData-PULL";

    @Test
    void verifyCanHandle() {
        assertThat(flowController.canHandle(transferProcess(HTTP_PROXY))).isTrue();
        assertThat(flowController.canHandle(transferProcess(HTTP_DATA_PULL, HTTP_DATA_PULL))).isTrue();
        assertThat(flowController.canHandle(transferProcess(HTTP_DATA_PULL, null))).isFalse();
        assertThat(flowController.canHandle(transferProcess("not-http-proxy"))).isFalse();
    }

    @Test
    void initiateFlow_success() {
        var proxyAddress = dataAddress();
        var instance = mock(DataPlaneInstance.class);
        var transferProcess = transferProcessBuilder(HTTP_PROXY)
                .contentDataAddress(dataAddress())
                .build();

        when(selectorService.select(any(), argThat(destination -> destination.getType().equals(HTTP_PROXY)))).thenReturn(instance);
        when(resolver.toDataAddress(any(), any(), any())).thenReturn(Result.success(proxyAddress));

        var result = flowController.start(transferProcess, null);

        assertThat(result).isSucceeded().satisfies(response -> {
            assertThat(response.getDataAddress()).isEqualTo(proxyAddress);
        });
    }

    @Test
    void initiateFlow_success_withTransferType() {
        var proxyAddress = dataAddress();
        var instance = mock(DataPlaneInstance.class);
        var transferProcess = transferProcessBuilder(HTTP_PROXY)
                .transferType(HTTP_DATA_PULL)
                .contentDataAddress(dataAddress())
                .build();

        when(selectorService.select(any(), argThat(destination -> destination.getType().equals(HTTP_PROXY)))).thenReturn(instance);
        when(resolver.toDataAddress(any(), any(), any())).thenReturn(Result.success(proxyAddress));

        var result = flowController.start(transferProcess, null);

        assertThat(result).isSucceeded().satisfies(response -> {
            assertThat(response.getDataAddress()).isEqualTo(proxyAddress);
        });
    }

    @Test
    void initiateFlow_returnsFailureIfNoDataPlaneInstance() {
        var transferProcess = transferProcessBuilder(HTTP_PROXY)
                .contentDataAddress(dataAddress())
                .build();

        var result = flowController.start(transferProcess, null);


        assertThat(result).isFailed().extracting(Failure::getFailureDetail).asString()
                .isEqualTo(String.format("Failed to find DataPlaneInstance for source/destination: %s/%s", transferProcess.getContentDataAddress().getType(), HTTP_PROXY));
    }

    @Test
    void initiateFlow_returnsFailureIfAddressResolutionFails() {
        var errorMsg = "Test Error Message";
        var instance = mock(DataPlaneInstance.class);
        var transferProcess = transferProcessBuilder(HTTP_PROXY)
                .contentDataAddress(dataAddress())
                .build();

        when(selectorService.select(any(), argThat(destination -> destination.getType().equals(HTTP_PROXY)))).thenReturn(instance);
        when(resolver.toDataAddress(any(), any(), any())).thenReturn(Result.failure(errorMsg));

        var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

        assertThat(result).isFailed().extracting(Failure::getFailureDetail).asString().contains(errorMsg);
    }

    @Test
    void terminate_shouldAlwaysReturnSuccess() {
        var transferProcess = transferProcessBuilder(HTTP_PROXY)
                .contentDataAddress(dataAddress())
                .build();

        var result = flowController.terminate(transferProcess);

        assertThat(result).isSucceeded();
    }

    @Test
    void transferTypes_shouldReturnHttpPull() {
        var asset = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("any").build()).build();

        var transferTypes = flowController.transferTypesFor(asset);

        assertThat(transferTypes).hasSize(1).contains(HTTP_DATA_PULL);
    }

    private TransferProcess transferProcess(String destinationType) {
        return transferProcess(destinationType, null);
    }

    private TransferProcess.Builder transferProcessBuilder(String destinationType) {
        return TransferProcess.Builder.newInstance()
                .correlationId(UUID.randomUUID().toString())
                .protocol("protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .counterPartyAddress("test.connector.address")
                .dataDestination(DataAddress.Builder.newInstance().type(destinationType).build());
    }

    private TransferProcess transferProcess(String destinationType, String transferType) {
        return transferProcessBuilder(destinationType)
                .transferType(transferType)
                .dataDestination(DataAddress.Builder.newInstance().type(destinationType).build())
                .build();
    }

    private DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
    }

}
