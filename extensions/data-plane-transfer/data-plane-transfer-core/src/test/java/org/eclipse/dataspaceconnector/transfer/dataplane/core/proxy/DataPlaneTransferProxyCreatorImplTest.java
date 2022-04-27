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
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyTokenGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferProxyCreatorImplTest {

    private static final Faker FAKER = new Faker();

    private String endpoint;
    private DataPlaneTransferProxyCreator proxyManager;
    private DataPlaneProxyTokenGenerator tokenGeneratorMock;

    @BeforeEach
    public void setUp() {
        endpoint = FAKER.internet().url();
        tokenGeneratorMock = mock(DataPlaneProxyTokenGenerator.class);
        proxyManager = new DataPlaneTransferProxyCreatorImpl(endpoint, tokenGeneratorMock);
    }

    /**
     * OK test: check proxy creation.
     */
    @Test
    void createProxy_success() {
        var id = FAKER.internet().uuid();
        var address = DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build();
        var contractId = FAKER.internet().uuid();
        var generatedToken = TokenRepresentation.Builder.newInstance().token(FAKER.internet().uuid()).build();

        when(tokenGeneratorMock.generate(address, contractId)).thenReturn(Result.success(generatedToken));

        var proxyCreationRequest = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .id(id)
                .address(address)
                .contractId(contractId)
                .property(FAKER.lorem().word(), FAKER.internet().uuid())
                .property(FAKER.lorem().word(), FAKER.internet().uuid())
                .build();

        var result = proxyManager.createProxy(proxyCreationRequest);

        verify(tokenGeneratorMock, times(1)).generate(any(), any());

        assertThat(result.succeeded()).isTrue();
        var edr = result.getContent();
        assertThat(edr.getId()).isEqualTo(id);
        assertThat(edr.getEndpoint()).isEqualTo(endpoint);
        assertThat(edr.getAuthKey()).isEqualTo(DataPlaneConstants.PUBLIC_API_AUTH_HEADER);
        assertThat(edr.getAuthCode()).isEqualTo(generatedToken.getToken());
        var expectedProperties = new HashMap<>(proxyCreationRequest.getProperties());
        expectedProperties.put(CONTRACT_ID, contractId);
        assertThat(edr.getProperties()).containsExactlyInAnyOrderEntriesOf(expectedProperties);
    }

    /**
     * Check that a failed result is returned if input {@link EndpointDataReference} does not contain a contract id.
     */
    @Test
    void tokenGenerationFails_shouldReturnFailedResult() {
        var errorMsg = FAKER.lorem().sentence();

        when(tokenGeneratorMock.generate(any(), any())).thenReturn(Result.failure(errorMsg));

        var result = proxyManager.createProxy(createDummyProxyCreationRequest());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(errorMsg);
    }

    private static DataPlaneTransferProxyCreationRequest createDummyProxyCreationRequest() {
        return DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .address(DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build())
                .contractId(FAKER.internet().uuid())
                .build();
    }
}