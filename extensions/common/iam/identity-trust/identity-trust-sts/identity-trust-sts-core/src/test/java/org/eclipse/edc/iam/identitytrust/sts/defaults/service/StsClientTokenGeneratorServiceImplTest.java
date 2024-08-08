/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.defaults.service;


import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsTokenGenerationProvider;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StsClientTokenGeneratorServiceImplTest {

    public static final long TOKEN_EXPIRATION = 60 * 5;
    private final StsTokenGenerationProvider tokenGenerationProvider = mock();
    private final TokenGenerationService tokenGenerator = mock();
    private StsClientTokenGeneratorServiceImpl clientTokenService;

    @BeforeEach
    void setup() {
        clientTokenService = new StsClientTokenGeneratorServiceImpl(tokenGenerationProvider, (client) -> "test-key-id", Clock.systemUTC(), TOKEN_EXPIRATION);
    }

    @Test
    void tokenFor() {
        var client = createClient("clientId");
        var token = TokenRepresentation.Builder.newInstance().token("token").expiresIn(TOKEN_EXPIRATION).build();
        when(tokenGenerationProvider.tokenGeneratorFor(client)).thenReturn(tokenGenerator);
        when(tokenGenerator.generate(any(), any(TokenDecorator[].class))).thenReturn(Result.success(token));

        var inserted = clientTokenService.tokenFor(client, StsClientTokenAdditionalParams.Builder.newInstance().audience("aud").build());

        assertThat(inserted).isSucceeded().usingRecursiveComparison().isEqualTo(token);
    }

    @Test
    void tokenFor_error_whenGeneratorFails() {
        var client = createClient("clientId");
        when(tokenGenerationProvider.tokenGeneratorFor(client)).thenReturn(tokenGenerator);
        when(tokenGenerator.generate(any(), any(TokenDecorator[].class))).thenReturn(Result.failure("failure"));

        var inserted = clientTokenService.tokenFor(client, StsClientTokenAdditionalParams.Builder.newInstance().audience("aud").build());

        assertThat(inserted).isFailed()
                .satisfies(serviceFailure -> {
                    assertThat(serviceFailure.getReason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
                    assertThat(serviceFailure.getFailureDetail()).isEqualTo("failure");
                });
    }

}
