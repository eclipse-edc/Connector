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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferConsumerProxyTransformerTest {

    private static final Faker FAKER = new Faker();

    private String proxyEndpoint;
    private DataPlaneTransferProxyReferenceService proxyCreatorMock;
    private DataPlaneTransferConsumerProxyTransformer transformer;

    @BeforeEach
    public void setUp() {
        proxyEndpoint = FAKER.internet().url();
        proxyCreatorMock = mock(DataPlaneTransferProxyReferenceService.class);
        transformer = new DataPlaneTransferConsumerProxyTransformer(proxyEndpoint, proxyCreatorMock);
    }

    /**
     * OK test: check that success result if returned and assert content of the returned {@link EndpointDataReference}
     */
    @Test
    void transformation_success() {
        var inputEdr = createEndpointDataReference();
        var outputEdr = createEndpointDataReference();
        var proxyCreationRequestCapture = ArgumentCaptor.forClass(DataPlaneTransferProxyCreationRequest.class);

        when(proxyCreatorMock.createProxyReference(any())).thenReturn(Result.success(outputEdr));

        var result = transformer.transform(inputEdr);

        verify(proxyCreatorMock, times(1)).createProxyReference(proxyCreationRequestCapture.capture());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(outputEdr);

        var proxyCreationRequest = proxyCreationRequestCapture.getValue();
        assertThat(proxyCreationRequest.getId()).isEqualTo(inputEdr.getId());
        assertThat(proxyCreationRequest.getContractId()).isEqualTo(inputEdr.getProperties().get(CONTRACT_ID));
        assertThat(proxyCreationRequest.getProxyEndpoint()).isEqualTo(proxyEndpoint);
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
                .endpoint(FAKER.internet().url())
                .authKey(FAKER.lorem().word())
                .authCode(FAKER.internet().uuid())
                .id(FAKER.internet().uuid())
                .properties(Map.of(FAKER.lorem().word(), FAKER.lorem().word()))
                .build();

        var result = transformer.transform(edr);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .containsExactly(String.format("Cannot transform endpoint data reference with id %s as contract id is missing", edr.getId()));
    }

    /**
     * Creates dummy {@link EndpointDataReference}.
     */
    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint(FAKER.internet().url())
                .authKey(FAKER.lorem().word())
                .authCode(FAKER.internet().uuid())
                .id(FAKER.internet().uuid())
                .properties(Map.of(
                        FAKER.lorem().word(), FAKER.lorem().word(),
                        CONTRACT_ID, FAKER.internet().uuid())
                )
                .build();
    }
}