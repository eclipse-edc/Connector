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

import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmbeddedSecureTokenServiceTest {

    private final TokenGenerationService tokenGenerationService = mock();
    private final Supplier<PrivateKey> keySupplier = () -> mock(PrivateKey.class);

    @Test
    void createToken_withoutBearerAccessScope() {
        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, Clock.systemUTC(), 10 * 60);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(keySupplier), any())).thenReturn(Result.success(token));
        var result = sts.createToken(Map.of(), null);

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(JwtDecorator.class);

        verify(tokenGenerationService).generate(any(), captor.capture());

        assertThat(captor.getAllValues()).hasSize(1)
                .hasOnlyElementsOfType(SelfIssuedTokenDecorator.class);
    }

    @Test
    void createToken_withBearerAccessScope() {

        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");
        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, Clock.systemUTC(), 10 * 60);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(keySupplier), any(JwtDecorator[].class)))
                .thenReturn(Result.success(token))
                .thenReturn(Result.success(token));


        var result = sts.createToken(claims, "scope:test");

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(JwtDecorator[].class);

        verify(tokenGenerationService, times(2)).generate(any(), captor.capture());

        assertThat(captor.getAllValues()).hasSize(2)
                .satisfies(list -> {
                    assertThat(list.get(0))
                            .hasSize(1)
                            .hasExactlyElementsOfTypes(SelfIssuedTokenDecorator.class);

                    assertThat(list.get(1))
                            .hasSize(1)
                            .hasExactlyElementsOfTypes(SelfIssuedTokenDecorator.class);
                });

    }

    @Test
    void createToken_error_whenAccessTokenFails() {

        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");

        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, Clock.systemUTC(), 10 * 60);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(keySupplier), any(JwtDecorator[].class)))
                .thenReturn(Result.failure("Failed to create access token"))
                .thenReturn(Result.success(token));

        var result = sts.createToken(claims, "scope:test");

        assertThat(result.failed()).isTrue();
        var captor = ArgumentCaptor.forClass(JwtDecorator[].class);

        verify(tokenGenerationService, times(1)).generate(any(), captor.capture());

        assertThat(captor.getValue()).hasSize(1)
                .hasExactlyElementsOfTypes(SelfIssuedTokenDecorator.class);

    }

    @Test
    void createToken_error_whenSelfTokenFails() {
        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");

        var sts = new EmbeddedSecureTokenService(tokenGenerationService, keySupplier, Clock.systemUTC(), 10 * 60);
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(keySupplier), any(JwtDecorator[].class)))
                .thenReturn(Result.success(token))
                .thenReturn(Result.failure("Failed to create access token"));


        var result = sts.createToken(claims, "scope:test");

        assertThat(result.failed()).isTrue();

        verify(tokenGenerationService, times(2)).generate(eq(keySupplier), any(JwtDecorator[].class));

    }

}
