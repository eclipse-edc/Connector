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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.remote.RemoteSecureTokenService.AUDIENCE_PARAM;
import static org.eclipse.edc.iam.identitytrust.sts.remote.RemoteSecureTokenService.GRANT_TYPE;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.BEARER_ACCESS_ALIAS;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.BEARER_ACCESS_SCOPE;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.PRESENTATION_ACCESS_TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoteSecureTokenServiceTest {

    private final StsRemoteClientConfiguration configuration = new StsRemoteClientConfiguration("id", "secret", "url");
    private final Oauth2Client oauth2Client = mock();
    private RemoteSecureTokenService secureTokenService;

    @BeforeEach
    void setup() {
        secureTokenService = new RemoteSecureTokenService(oauth2Client, configuration);
    }

    @Test
    void createToken() {
        var audience = "aud";
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience), null)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(configuration.clientSecret());
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience);
        });
    }

    @Test
    void createToken_withAccessScope() {
        var audience = "aud";
        var bearerAccessScope = "scope";
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience), bearerAccessScope)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(configuration.clientSecret());
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(BEARER_ACCESS_SCOPE, bearerAccessScope);
        });
    }

    @Test
    void createToken_withAccessToken() {
        var audience = "aud";
        var accessToken = "accessToken";
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        assertThat(secureTokenService.createToken(Map.of(AUDIENCE, audience, PRESENTATION_ACCESS_TOKEN_CLAIM, accessToken), null)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(configuration.clientSecret());
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(PRESENTATION_ACCESS_TOKEN_CLAIM, accessToken);
        });
    }

    @Test
    void createToken_withBearerAccessTokenAlias() {
        var audience = "aud";
        var bearerAccessScope = "scope";
        var bearerAccessAlias = "alias";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));

        var claims = Map.of(
                AUDIENCE, audience,
                BEARER_ACCESS_ALIAS, bearerAccessAlias);

        assertThat(secureTokenService.createToken(claims, bearerAccessScope)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.getUrl()).isEqualTo(configuration.tokenUrl());
            assertThat(request.getClientId()).isEqualTo(configuration.clientId());
            assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            assertThat(request.getClientSecret()).isEqualTo(configuration.clientSecret());
            assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(BEARER_ACCESS_ALIAS, bearerAccessAlias)
                    .containsEntry(BEARER_ACCESS_SCOPE, bearerAccessScope);
        });
    }
}
