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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.transformer;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAIM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyEndpointDataReferenceTransformerTest {
    private static final String DATA_ENDPOINT = "http://dataplane.com/api/data";

    private ProxyEndpointDataReferenceTransformer transformer;
    private TokenGenerationService tokenGenerationServiceMock;
    private DataEncrypter dataEncrypterMock;
    private TypeManager typeManager;

    @BeforeEach
    public void setUp() {
        tokenGenerationServiceMock = mock(TokenGenerationService.class);
        typeManager = new TypeManager();
        dataEncrypterMock = mock(DataEncrypter.class);
        transformer = new ProxyEndpointDataReferenceTransformer(tokenGenerationServiceMock, dataEncrypterMock, DATA_ENDPOINT, typeManager);
    }

    /**
     * OK test: check that success result if returned and assert content of the returned {@link EndpointDataReference}
     */
    @Test
    void transformationSuccessful() {
        var edr = createEndpointDataReference();
        var generatedToken = TokenRepresentation.Builder.newInstance().token(UUID.randomUUID().toString()).build();
        var claimsCapture = ArgumentCaptor.forClass(JWTClaimsSet.class);
        var daCapture = ArgumentCaptor.forClass(String.class);
        when(tokenGenerationServiceMock.generate(claimsCapture.capture())).thenReturn(Result.success(generatedToken));
        when(dataEncrypterMock.encrypt(daCapture.capture())).thenReturn("encrypted-data-address");

        var result = transformer.execute(edr);

        assertThat(claimsCapture.getValue()).satisfies(claimsSet -> {
            assertThat(claimsSet.getExpirationTime()).isNotNull().isAfter(Instant.now());
            assertThat(claimsSet.getClaim(CONTRACT_ID_CLAIM)).hasToString(edr.getContractId());
            assertThat(claimsSet.getClaim(DATA_ADDRESS_CLAIM)).hasToString("encrypted-data-address");
        });

        var da = typeManager.readValue(daCapture.getValue(), DataAddress.class);
        assertThat(da.getType()).isEqualTo("HttpData");
        assertThat(da.getProperties())
                .containsEntry("endpoint", edr.getAddress())
                .containsEntry("authKey", edr.getAuthKey())
                .containsEntry("authCode", edr.getAuthCode());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent())
                .satisfies(e -> {
                    assertThat(e.getCorrelationId()).isEqualTo(edr.getCorrelationId());
                    assertThat(e.getAddress()).isEqualTo(DATA_ENDPOINT);
                    assertThat(e.getAuthKey()).isEqualTo(edr.getAuthKey());
                    assertThat(e.getAuthCode()).isEqualTo(generatedToken.getToken());
                    assertThat(e.getContractId()).isEqualTo(edr.getContractId());
                    assertThat(e.getExpirationEpochSeconds()).isEqualTo(edr.getExpirationEpochSeconds());
                });
    }

    /**
     * Check that a failed result is result if transformation failed.
     */
    @Test
    void transformationFailure() {
        var edr = createEndpointDataReference();
        when(tokenGenerationServiceMock.generate(any())).thenReturn(Result.failure("error"));
        when(dataEncrypterMock.encrypt(anyString())).thenReturn("encrypted-data-address");

        var result = transformer.execute(edr);

        assertThat(result.failed()).isTrue();
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .address("http://example.com")
                .authKey("Api-Key")
                .authCode(UUID.randomUUID().toString())
                .correlationId("correlation-test")
                .contractId(UUID.randomUUID().toString())
                .expirationEpochSeconds(Instant.now().plusSeconds(100).getEpochSecond())
                .build();
    }
}