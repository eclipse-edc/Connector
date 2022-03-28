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

import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneProxyManagerImplTest {
    private static final String DATA_ENDPOINT = "http://dataplane.com/api/data";

    private DataPlaneProxyManager proxyManager;
    private DataPlaneProxyTokenGenerator tokenGeneratorMock;

    @BeforeEach
    public void setUp() {
        tokenGeneratorMock = mock(DataPlaneProxyTokenGenerator.class);
        proxyManager = new DataPlaneProxyManagerImpl(DATA_ENDPOINT, tokenGeneratorMock);
    }

    /**
     * OK test: check that success result if returned and assert content of the returned {@link EndpointDataReference}
     */
    @Test
    void transformation_success() {
        var edr = createEndpointDataReference();
        var generatedToken = TokenRepresentation.Builder.newInstance().token("generated-token-test").build();

        var dataAddressCapture = ArgumentCaptor.forClass(DataAddress.class);
        var contractIdCapture = ArgumentCaptor.forClass(String.class);

        when(tokenGeneratorMock.generate(any(), anyString())).thenReturn(Result.success(generatedToken));

        var result = proxyManager.transform(edr);

        verify(tokenGeneratorMock, times(1)).generate(dataAddressCapture.capture(), contractIdCapture.capture());

        assertThat(contractIdCapture.getValue()).isEqualTo("123");
        var address = dataAddressCapture.getValue();
        assertThat(address.getType()).isEqualTo(TYPE);
        assertThat(address.getProperties())
                .containsEntry(ENDPOINT, edr.getEndpoint())
                .containsEntry(AUTHENTICATION_KEY, edr.getAuthKey())
                .containsEntry(AUTHENTICATION_CODE, edr.getAuthCode());

        assertThat(result.succeeded()).isTrue();
        var transformedEdr = result.getContent();
        assertThat(transformedEdr.getId()).isEqualTo(edr.getId());
        assertThat(transformedEdr.getEndpoint()).isEqualTo(DATA_ENDPOINT);
        assertThat(transformedEdr.getAuthKey()).isEqualTo("Authorization");
        assertThat(transformedEdr.getAuthCode()).isEqualTo(generatedToken.getToken());
        assertThat(transformedEdr.getProperties()).isEqualTo(edr.getProperties());
    }

    /**
     * OK test: check proxy creation.
     */
    @Test
    void createProxy_success() {
        var id = UUID.randomUUID().toString();
        var address = DataAddress.Builder.newInstance().type("test").build();
        var contractId = "contract-test";
        var generatedToken = TokenRepresentation.Builder.newInstance().token("generated-token-test").build();

        var dataAddressCapture = ArgumentCaptor.forClass(DataAddress.class);
        var contractIdCapture = ArgumentCaptor.forClass(String.class);

        when(tokenGeneratorMock.generate(any(), anyString())).thenReturn(Result.success(generatedToken));

        var result = proxyManager.createProxy(DataPlaneProxyCreationRequest.Builder.newInstance()
                .id(id)
                .address(address)
                .contractId(contractId)
                .property("foo", "bar")
                .property("hello", "world")
                .build());

        verify(tokenGeneratorMock, times(1)).generate(dataAddressCapture.capture(), contractIdCapture.capture());

        assertThat(contractIdCapture.getValue()).isEqualTo(contractId);
        assertThat(dataAddressCapture.getValue().getType()).isEqualTo("test");

        assertThat(result.succeeded()).isTrue();
        var edr = result.getContent();
        assertThat(edr.getId()).isEqualTo(id);
        assertThat(edr.getEndpoint()).isEqualTo(DATA_ENDPOINT);
        assertThat(edr.getAuthKey()).isEqualTo("Authorization");
        assertThat(edr.getAuthCode()).isEqualTo(generatedToken.getToken());
        assertThat(edr.getProperties())
                .containsExactlyInAnyOrderEntriesOf(Map.of("foo", "bar", "hello", "world", CONTRACT_ID, "contract-test"));
    }

    /**
     * Check that a failed result is returned if input {@link EndpointDataReference} does not contain a contract id.
     */
    @Test
    void missingContractId_shouldReturnFailedResult() {
        var edr = EndpointDataReference.Builder.newInstance()
                .endpoint("http://example.com")
                .authKey("Api-Key")
                .authCode("token-test")
                .id(UUID.randomUUID().toString())
                .properties(Map.of("foo", "bar"))
                .build();

        var result = proxyManager.transform(edr);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .containsExactly(String.format("Cannot transform endpoint data reference with id %s as contract id is missing", edr.getId()));
    }

    /**
     * Check that a failed result is returned if input {@link EndpointDataReference} does not contain a contract id.
     */
    @Test
    void tokenGenerationFails_shouldReturnFailedResult() {
        var edr = createEndpointDataReference();

        when(tokenGeneratorMock.generate(any(), any())).thenReturn(Result.failure("error"));

        var result = proxyManager.transform(edr);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("error");
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("http://example.com")
                .authKey("Api-Key")
                .authCode("token-test")
                .id(UUID.randomUUID().toString())
                .properties(Map.of("foo", "bar", CONTRACT_ID, "123"))
                .build();
    }
}