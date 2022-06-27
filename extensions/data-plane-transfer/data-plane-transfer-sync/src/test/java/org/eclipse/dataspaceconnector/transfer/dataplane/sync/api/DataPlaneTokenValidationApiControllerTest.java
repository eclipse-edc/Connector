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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.api;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.exception.NotAuthorizedException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.DATA_ADDRESS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTokenValidationApiControllerTest {

    private static final Faker FAKER = new Faker();

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private final DataEncrypter encrypterMock = mock(DataEncrypter.class);
    private final TokenValidationService tokenValidationServiceMock = mock(TokenValidationService.class);

    private DataPlaneTokenValidationApiController controller;

    @BeforeEach
    void setUp() {
        controller = new DataPlaneTokenValidationApiController(tokenValidationServiceMock, encrypterMock, TYPE_MANAGER);
    }

    @Test
    void verifyValidateSuccess() {
        var token = FAKER.internet().uuid();
        var encryptedDataAddress = FAKER.internet().uuid();
        var decryptedDataAddress = DataAddress.Builder.newInstance().type(FAKER.internet().uuid()).build();
        var claims = ClaimToken.Builder.newInstance()
                .claims(Map.of(
                                FAKER.lorem().word(), FAKER.lorem().word(),
                                DATA_ADDRESS, encryptedDataAddress
                        )
                )
                .build();

        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));
        when(encrypterMock.decrypt(encryptedDataAddress)).thenReturn(TYPE_MANAGER.writeValueAsString(decryptedDataAddress));

        var responseAddress = controller.validate(token);
        assertThat(responseAddress.getType()).isEqualTo(decryptedDataAddress.getType());

        verify(tokenValidationServiceMock).validate(anyString());
        verify(encrypterMock).decrypt(anyString());
    }

    @Test
    void verifyTokenValidationFailureThrowsException() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();

        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.failure(errorMsg));

        assertThatExceptionOfType(NotAuthorizedException.class).isThrownBy(() -> controller.validate(token));

        verify(encrypterMock, never()).decrypt(anyString());
    }

    @Test
    void verifyMissingAddressThrowsException() {
        var token = FAKER.internet().uuid();
        var claims = ClaimToken.Builder.newInstance()
                .claims(Map.of(
                                FAKER.lorem().word(), FAKER.lorem().word()
                        )
                )
                .build();

        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> controller.validate(token));

        verify(encrypterMock, never()).decrypt(anyString());
    }
}