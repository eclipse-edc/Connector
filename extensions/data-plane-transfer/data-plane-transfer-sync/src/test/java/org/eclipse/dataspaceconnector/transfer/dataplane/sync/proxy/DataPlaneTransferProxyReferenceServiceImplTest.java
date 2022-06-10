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
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.DATA_ADDRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferProxyReferenceServiceImplTest {

    private static final Faker FAKER = new Faker();

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private final Instant now = Instant.now();
    private long tokenValiditySeconds;
    private DataPlaneTransferProxyReferenceService proxyManager;
    private TokenGenerationService tokenGeneratorMock;
    private DataEncrypter encrypterMock;

    @BeforeEach
    public void setUp() {
        tokenGeneratorMock = mock(TokenGenerationService.class);
        tokenValiditySeconds = FAKER.random().nextLong(100);
        encrypterMock = mock(DataEncrypter.class);
        Clock clock = Clock.fixed(now, UTC);
        proxyManager = new DataPlaneTransferProxyReferenceServiceImpl(tokenGeneratorMock, TYPE_MANAGER, tokenValiditySeconds, encrypterMock, clock);
    }

    /**
     * OK test: check proxy creation.
     */
    @Test
    void createProxy_success() throws ParseException {
        var id = FAKER.internet().uuid();
        var address = DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build();
        var addressStr = TYPE_MANAGER.writeValueAsString(address);
        var encryptedDataAddress = FAKER.internet().uuid();
        var contractId = FAKER.internet().uuid();
        var proxyEndpoint = FAKER.internet().url();
        var generatedToken = TokenRepresentation.Builder.newInstance().token(FAKER.internet().uuid()).build();

        var decoratorCaptor = ArgumentCaptor.forClass(DataPlaneProxyTokenDecorator.class);

        when(encrypterMock.encrypt(addressStr)).thenReturn(encryptedDataAddress);
        when(tokenGeneratorMock.generate(any())).thenReturn(Result.success(generatedToken));

        var proxyCreationRequest = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .id(id)
                .contentAddress(address)
                .contractId(contractId)
                .proxyEndpoint(proxyEndpoint)
                .property(FAKER.lorem().word(), FAKER.internet().uuid())
                .property(FAKER.lorem().word(), FAKER.internet().uuid())
                .build();

        var result = proxyManager.createProxyReference(proxyCreationRequest);

        verify(tokenGeneratorMock).generate(decoratorCaptor.capture());

        var decorator = decoratorCaptor.getValue();

        // test the decorator
        var builder = new JWTClaimsSet.Builder();
        decorator.decorate(null, builder);
        var claims = builder.build();
        assertThat(claims.getStringClaim(CONTRACT_ID)).isEqualTo(contractId);
        assertThat(claims.getStringClaim(DATA_ADDRESS)).isEqualTo(encryptedDataAddress);
        assertThat(claims.getExpirationTime()).isEqualTo(Date.from(now.plusSeconds(tokenValiditySeconds)));

        assertThat(result.succeeded()).isTrue();
        var edr = result.getContent();
        assertThat(edr.getId()).isEqualTo(id);
        assertThat(edr.getEndpoint()).isEqualTo(proxyEndpoint);
        assertThat(edr.getAuthKey()).isEqualTo(HttpHeaders.AUTHORIZATION);
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
        var request = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .contentAddress(DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build())
                .contractId(FAKER.internet().uuid())
                .proxyEndpoint(FAKER.internet().url())
                .build();

        when(tokenGeneratorMock.generate(any(DataPlaneProxyTokenDecorator.class))).thenReturn(Result.failure(errorMsg));

        var result = proxyManager.createProxyReference(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(errorMsg);
    }
}