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

package org.eclipse.dataspaceconnector.transfer.dataplane.core.proxy;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferConsumerProxyTransformerTest {

    private static final Faker FAKER = new Faker();

    private DataPlaneTransferProxyCreator proxyCreatorMock;
    private DataPlaneTransferConsumerProxyTransformer transformer;

    @BeforeEach
    public void setUp() {
        proxyCreatorMock = mock(DataPlaneTransferProxyCreator.class);
        transformer = new DataPlaneTransferConsumerProxyTransformer(proxyCreatorMock);
    }

    /**
     * OK test: check that success result if returned and assert content of the returned {@link EndpointDataReference}
     */
    @Test
    void transformation_success() {
        var inputEdr = createEndpointDataReference();
        var outputEdr = createEndpointDataReference();
        var proxyCreationRequestCapture = ArgumentCaptor.forClass(DataPlaneTransferProxyCreationRequest.class);

        when(proxyCreatorMock.createProxy(any())).thenReturn(Result.success(outputEdr));

        var result = transformer.transform(inputEdr);

        verify(proxyCreatorMock, times(1)).createProxy(proxyCreationRequestCapture.capture());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(outputEdr);

        var proxyCreationRequest = proxyCreationRequestCapture.getValue();
        assertThat(proxyCreationRequest.getId()).isEqualTo(inputEdr.getId());
        assertThat(proxyCreationRequest.getContractId()).isEqualTo(inputEdr.getProperties().get(CONTRACT_ID));
        assertThat(proxyCreationRequest.getProperties()).containsExactlyInAnyOrderEntriesOf(inputEdr.getProperties());
        assertThat(proxyCreationRequest.getAddress()).satisfies(address -> {
            assertThat(address.getType()).isEqualTo(TYPE);
            assertThat(address.getProperty(ENDPOINT)).isEqualTo(inputEdr.getEndpoint());
            assertThat(address.getProperty(AUTHENTICATION_KEY)).isEqualTo(inputEdr.getAuthKey());
            assertThat(address.getProperty(AUTHENTICATION_CODE)).isEqualTo(inputEdr.getAuthCode());
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