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
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyAccessManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.DATA_ADDRESS;
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

class DataPlaneProxyAccessManagerImplTest {

    private static final Faker FAKER = new Faker();
    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private String dataEndpoint;

    private DataEncrypter dataEncrypterMock;
    private DataPlaneTransferTokenGenerator tokenGeneratorMock;
    private long tokenValiditySeconds;

    private DataPlaneProxyAccessManager proxyManager;

    @BeforeEach
    public void setUp() {
        dataEndpoint = FAKER.internet().url();
        tokenGeneratorMock = mock(DataPlaneTransferTokenGenerator.class);
        dataEncrypterMock = mock(DataEncrypter.class);
        tokenValiditySeconds = FAKER.random().nextInt(100);
        proxyManager = new DataPlaneProxyAccessManagerImpl(dataEndpoint, tokenGeneratorMock, TYPE_MANAGER, dataEncrypterMock, tokenValiditySeconds);
    }

    /**
     * OK test: check that success result if returned and assert content of the returned {@link EndpointDataReference}
     */
    @Test
    void transformation_success() throws ParseException {
        var contractId = FAKER.internet().uuid();
        var edr = createEndpointDataReference(contractId);
        var generatedToken = TokenRepresentation.Builder.newInstance().token(FAKER.internet().uuid()).build();
        var edrAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, edr.getEndpoint())
                .property(AUTHENTICATION_KEY, edr.getAuthKey())
                .property(AUTHENTICATION_CODE, edr.getAuthCode())
                .build();
        var addressStr = TYPE_MANAGER.writeValueAsString(edrAddress);
        var encryptedDataAddress = FAKER.internet().uuid();

        var decoratorCapture = ArgumentCaptor.forClass(DataPlaneProxyTokenDecorator.class);

        when(dataEncrypterMock.encrypt(addressStr)).thenReturn(encryptedDataAddress);
        when(tokenGeneratorMock.generate(any(DataPlaneProxyTokenDecorator.class))).thenReturn(Result.success(generatedToken));

        var result = proxyManager.transform(edr);

        verify(tokenGeneratorMock, times(1)).generate(decoratorCapture.capture());
        verify(dataEncrypterMock, times(1)).encrypt(anyString());

        verifyDecorator(decoratorCapture.getValue(), contractId, encryptedDataAddress, Date.from(Instant.now().plusSeconds(tokenValiditySeconds)));

        assertThat(result.succeeded()).isTrue();
        var transformedEdr = result.getContent();
        assertThat(transformedEdr.getId()).isEqualTo(edr.getId());
        assertThat(transformedEdr.getEndpoint()).isEqualTo(dataEndpoint);
        assertThat(transformedEdr.getAuthKey()).isEqualTo(DataPlaneConstants.PUBLIC_API_AUTH_HEADER);
        assertThat(transformedEdr.getAuthCode()).isEqualTo(generatedToken.getToken());
        assertThat(transformedEdr.getProperties()).isEqualTo(edr.getProperties());
    }

    /**
     * OK test: check proxy creation.
     */
    @Test
    void createProxy_success() throws ParseException {
        var id = FAKER.internet().uuid();
        var address = DataAddress.Builder.newInstance().type(FAKER.internet().uuid()).build();
        var addressStr = TYPE_MANAGER.writeValueAsString(address);
        var encryptedDataAddress = FAKER.internet().uuid();
        var contractId = FAKER.internet().uuid();
        var generatedToken = TokenRepresentation.Builder.newInstance().token(FAKER.internet().uuid()).build();

        var decoratorCapture = ArgumentCaptor.forClass(DataPlaneProxyTokenDecorator.class);

        when(dataEncrypterMock.encrypt(addressStr)).thenReturn(encryptedDataAddress);
        when(tokenGeneratorMock.generate(any())).thenReturn(Result.success(generatedToken));

        var result = proxyManager.createProxy(DataPlaneProxyCreationRequest.Builder.newInstance()
                .id(id)
                .address(address)
                .contractId(contractId)
                .property("foo", "bar")
                .property("hello", "world")
                .build());

        verify(dataEncrypterMock, times(1)).encrypt(anyString());
        verify(tokenGeneratorMock, times(1)).generate(decoratorCapture.capture());

        verifyDecorator(decoratorCapture.getValue(), contractId, encryptedDataAddress, Date.from(Instant.now().plusSeconds(tokenValiditySeconds)));

        assertThat(result.succeeded()).isTrue();
        var edr = result.getContent();
        assertThat(edr.getId()).isEqualTo(id);
        assertThat(edr.getEndpoint()).isEqualTo(dataEndpoint);
        assertThat(edr.getAuthKey()).isEqualTo(DataPlaneConstants.PUBLIC_API_AUTH_HEADER);
        assertThat(edr.getAuthCode()).isEqualTo(generatedToken.getToken());
        assertThat(edr.getProperties())
                .containsExactlyInAnyOrderEntriesOf(Map.of("foo", "bar", "hello", "world", CONTRACT_ID, contractId));
    }

    /**
     * Check that a failed result is returned if input {@link EndpointDataReference} does not contain a contract id.
     */
    @Test
    void missingContractId_shouldReturnFailedResult() {
        var edr = EndpointDataReference.Builder.newInstance()
                .endpoint(dataEndpoint)
                .authKey(FAKER.internet().uuid())
                .authCode(FAKER.internet().uuid())
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
        var contractId = FAKER.internet().uuid();
        var edr = createEndpointDataReference(contractId);
        var errorMsg = FAKER.internet().uuid();

        when(tokenGeneratorMock.generate(any(DataPlaneProxyTokenDecorator.class))).thenReturn(Result.failure(errorMsg));

        var result = proxyManager.transform(edr);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(errorMsg);
    }

    /**
     * Verify that decorator behaves as expected.
     */
    private void verifyDecorator(JwtDecorator decorator, String expectedContractId, String expectedEncryptedDataAddress, Date expectedExpiration) throws ParseException {
        var builder = new JWTClaimsSet.Builder();

        decorator.decorate(null, builder);

        var claims = builder.build();
        assertThat(claims.getStringClaim(CONTRACT_ID)).isEqualTo(expectedContractId);
        assertThat(claims.getStringClaim(DATA_ADDRESS)).isEqualTo(expectedEncryptedDataAddress);
        assertThat(claims.getExpirationTime()).isNotNull().isCloseTo(expectedExpiration, 5000);

    }

    private static EndpointDataReference createEndpointDataReference(String contractId) {
        return EndpointDataReference.Builder.newInstance()
                .endpoint(FAKER.internet().url())
                .authKey(FAKER.internet().uuid())
                .authCode(FAKER.internet().uuid())
                .id(UUID.randomUUID().toString())
                .properties(Map.of("foo", "bar", CONTRACT_ID, contractId))
                .build();
    }
}