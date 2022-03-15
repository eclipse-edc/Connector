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

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyTokenGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.text.ParseException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.DATA_ADDRESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataPlaneProxyTokenGeneratorImplTest {

    private static final long TOKEN_VALIDITY_SECONDS = 100;

    private TypeManager typeManager;
    private DataEncrypter encrypterMock;
    private TokenGenerationService tokenGenerationServiceMock;

    private DataPlaneProxyTokenGenerator tokenGenerator;

    @BeforeEach
    public void setUp() {
        typeManager = new TypeManager();
        encrypterMock = mock(DataEncrypter.class);
        tokenGenerationServiceMock = mock(TokenGenerationService.class);
        tokenGenerator = new DataPlaneProxyTokenGeneratorImpl(typeManager, encrypterMock, tokenGenerationServiceMock, TOKEN_VALIDITY_SECONDS);
    }

    @Test
    void verifyTokenGenerationSuccess() throws ParseException {
        var contractId = "contract-test";
        var address = testDataAddress();
        var addressStr = typeManager.writeValueAsString(address);
        var token = TokenRepresentation.Builder.newInstance().token("token-test").build();

        var decoratorCapture = ArgumentCaptor.forClass(JwtDecorator.class);
        when(encrypterMock.encrypt(addressStr)).thenReturn("encrypted-data-address");
        when(tokenGenerationServiceMock.generate(decoratorCapture.capture())).thenReturn(Result.success(token));

        var result = tokenGenerator.generate(address, contractId);

        var decorator = decoratorCapture.getValue();
        // test the decorator
        var builder = new JWTClaimsSet.Builder();
        decorator.decorate(null, builder);
        var claims = builder.build();
        assertThat(claims.getStringClaim(CONTRACT_ID)).isEqualTo(contractId);
        assertThat(claims.getStringClaim(DATA_ADDRESS)).isEqualTo("encrypted-data-address");
        assertThat(claims.getExpirationTime()).isNotNull()
                .isCloseTo(Instant.now().plusSeconds(TOKEN_VALIDITY_SECONDS), 5000);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(token);
    }

    private static DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("dummy").build();
    }
}