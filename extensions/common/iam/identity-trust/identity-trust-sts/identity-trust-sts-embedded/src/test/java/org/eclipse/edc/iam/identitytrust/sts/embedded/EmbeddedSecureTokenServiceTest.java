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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmbeddedSecureTokenServiceTest {

    public static final String TEST_PRIVATEKEY_ID = "test-privatekey-id";
    private final TokenGenerationService tokenGenerationService = mock();
    private final Supplier<String> keySupplier = () -> TEST_PRIVATEKEY_ID;
    private final JtiValidationStore jtiValidationStore = mock();

    @BeforeEach
    void setup() {
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.success());
    }

    @Test
    void createToken_withoutBearerAccessScope() {
        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, () -> "test-key", Clock.systemUTC(), 10 * 60, jtiValidationStore);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class))).thenReturn(Result.success(token));
        var result = sts.createToken(Map.of(), null);

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);

        verify(tokenGenerationService).generate(any(), captor.capture());

        assertThat(captor.getAllValues()).hasSize(1)
                .allSatisfy(decorators -> {
                    assertThat(decorators).hasSize(2)
                            .hasOnlyElementsOfTypes(KeyIdDecorator.class, SelfIssuedTokenDecorator.class);
                });

    }

    @Test
    void createToken_withBearerAccessScope() {

        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");
        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, () -> "test-key", Clock.systemUTC(), 10 * 60, jtiValidationStore);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class)))
                .thenReturn(Result.success(token))
                .thenReturn(Result.success(token));


        var result = sts.createToken(claims, "scope:test");

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);

        verify(tokenGenerationService, times(2)).generate(any(), captor.capture());

        assertThat(captor.getAllValues()).hasSize(2)
                .satisfies(decorators -> {
                    assertThat(decorators.get(0))
                            .hasSize(2)
                            .hasOnlyElementsOfTypes(KeyIdDecorator.class, AccessTokenDecorator.class, SelfIssuedTokenDecorator.class);

                    assertThat(decorators.get(1))
                            .hasSize(2)
                            .hasOnlyElementsOfTypes(KeyIdDecorator.class, AccessTokenDecorator.class, SelfIssuedTokenDecorator.class);
                });

    }

    @Test
    void createToken_error_whenAccessTokenFails() {

        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");

        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, () -> "test-key", Clock.systemUTC(), 10 * 60, jtiValidationStore);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class)))
                .thenReturn(Result.failure("Failed to create access token"))
                .thenReturn(Result.success(token));

        var result = sts.createToken(claims, "scope:test");

        assertThat(result.failed()).isTrue();
        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);

        verify(tokenGenerationService, times(1)).generate(any(), captor.capture());

        assertThat(captor.getValue())
                .hasSize(2)
                .hasOnlyElementsOfTypes(SelfIssuedTokenDecorator.class, AccessTokenDecorator.class, KeyIdDecorator.class);

    }

    @Test
    void createToken_error_whenSelfTokenFails() {
        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");

        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, () -> "test-key", Clock.systemUTC(), 10 * 60, jtiValidationStore);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class)))
                .thenReturn(Result.success(token))
                .thenReturn(Result.failure("Failed to create access token"));


        var result = sts.createToken(claims, "scope:test");

        assertThat(result.failed()).isTrue();

        verify(tokenGenerationService, times(2)).generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class));

    }

}
