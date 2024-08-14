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

package org.eclipse.edc.iam.identitytrust.sts.remote;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.BEARER_ACCESS_SCOPE;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.iam.identitytrust.sts.remote.RemoteSecureTokenService.AUDIENCE_PARAM;
import static org.eclipse.edc.iam.identitytrust.sts.remote.RemoteSecureTokenService.GRANT_TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoteSecureTokenServiceTest {

    private final StsRemoteClientConfiguration configuration = new StsRemoteClientConfiguration("url", "id", "secretAlias");
    private final Oauth2Client oauth2Client = mock();
    private final Vault vault = mock();
    private RemoteSecureTokenService secureTokenService;

    @BeforeEach
    void setup() {
        secureTokenService = new RemoteSecureTokenService(oauth2Client, configuration, vault);
    }

    @Test
    void createToken() {
        var audience = "aud";
        var secret = "secret";
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(configuration.clientSecretAlias())).thenReturn(secret);

        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience), null)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(secret);
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience);
        });
    }

    @Test
    void createToken_withAccessScope() {
        var audience = "aud";
        var bearerAccessScope = "scope";
        var secret = "secret";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(configuration.clientSecretAlias())).thenReturn(secret);

        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience), bearerAccessScope)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(secret);
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(BEARER_ACCESS_SCOPE, bearerAccessScope);
        });
    }

    @Test
    void createToken_withAccessToken() {
        var audience = "aud";
        var accessToken = "accessToken";
        var secret = "secret";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(configuration.clientSecretAlias())).thenReturn(secret);

        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience, PRESENTATION_TOKEN_CLAIM, accessToken), null)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(secret);
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(PRESENTATION_TOKEN_CLAIM, accessToken);
        });
    }

    @Test
    void createToken_shouldFail_whenSecretIsNotPresent() {
        var audience = "aud";
        var accessToken = "accessToken";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(configuration.clientSecretAlias())).thenReturn(null);
        
        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience, PRESENTATION_TOKEN_CLAIM, accessToken), null))
                .isFailed()
                .detail().isEqualTo(format("Failed to fetch client secret from the vault with alias: %s", configuration.clientSecretAlias()));

    }

}
