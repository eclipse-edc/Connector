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

package org.eclipse.edc.connector.dataplane.transfer.sync.proxy;

import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.CONTRACT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferConsumerProxyTransformerTest {

    private DataPlaneTransferProxyResolver proxyResolverMock;
    private DataPlaneTransferProxyReferenceService proxyReferenceServiceMock;
    private DataPlaneTransferConsumerProxyTransformer transformer;

    /**
     * Creates dummy {@link EndpointDataReference}.
     */
    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("some.endpoint.url")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .properties(Map.of(
                        "key1", "value1",
                        CONTRACT_ID, UUID.randomUUID().toString())
                )
                .build();
    }

    @BeforeEach
    public void setUp() {
        proxyResolverMock = mock(DataPlaneTransferProxyResolver.class);
        proxyReferenceServiceMock = mock(DataPlaneTransferProxyReferenceService.class);
        transformer = new DataPlaneTransferConsumerProxyTransformer(proxyResolverMock, proxyReferenceServiceMock);
    }

    /**
     * OK test: check that success result if returned and assert content of the returned {@link EndpointDataReference}
     */
    @Test
    void transformation_success() {
        var inputEdr = createEndpointDataReference();
        var outputEdr = createEndpointDataReference();
        var proxyCreationRequestCapture = ArgumentCaptor.forClass(DataPlaneTransferProxyCreationRequest.class);
        var proxyUrl = "some.proxy.url";

        when(proxyResolverMock.resolveProxyUrl(any())).thenReturn(Result.success(proxyUrl));
        when(proxyReferenceServiceMock.createProxyReference(any())).thenReturn(Result.success(outputEdr));

        var result = transformer.transform(inputEdr);

        verify(proxyReferenceServiceMock).createProxyReference(proxyCreationRequestCapture.capture());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(outputEdr);

        var proxyCreationRequest = proxyCreationRequestCapture.getValue();
        assertThat(proxyCreationRequest.getId()).isEqualTo(inputEdr.getId());
        assertThat(proxyCreationRequest.getContractId()).isEqualTo(inputEdr.getProperties().get(CONTRACT_ID));
        assertThat(proxyCreationRequest.getProxyEndpoint()).isEqualTo(proxyUrl);
        assertThat(proxyCreationRequest.getProperties()).containsExactlyInAnyOrderEntriesOf(inputEdr.getProperties());
        assertThat(proxyCreationRequest.getContentAddress()).satisfies(address -> {
            assertThat(address.getType()).isEqualTo(HttpDataAddress.DATA_TYPE);
            var httpAddress = (HttpDataAddress) address;
            assertThat(httpAddress.getBaseUrl()).isEqualTo(inputEdr.getEndpoint());
            assertThat(httpAddress.getAuthKey()).isEqualTo(inputEdr.getAuthKey());
            assertThat(httpAddress.getAuthCode()).isEqualTo(inputEdr.getAuthCode());
            assertThat(httpAddress.getProxyQueryParams()).isEqualTo(Boolean.TRUE.toString());
            assertThat(httpAddress.getProxyPath()).isEqualTo(Boolean.TRUE.toString());
            assertThat(httpAddress.getProxyMethod()).isEqualTo(Boolean.TRUE.toString());
            assertThat(httpAddress.getProxyBody()).isEqualTo(Boolean.TRUE.toString());
        });
    }

    /**
     * Check that a failed result is returned if input {@link EndpointDataReference} does not contain a contract id.
     */
    @Test
    void missingContractId_shouldReturnFailedResult() {
        var edr = EndpointDataReference.Builder.newInstance()
                .endpoint("some.test.endpoint")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .properties(Map.of("key1", "value1"))
                .build();

        var result = transformer.transform(edr);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .containsExactly(String.format("Cannot transform endpoint data reference with id %s as contract id is missing", edr.getId()));
    }
}