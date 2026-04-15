/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2.logic;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Oauth2CredentialsSignalingAuthorizationTest {

    private final Oauth2Client oauth2Client = mock();
    private final TokenValidationService tokenValidationService = mock();
    private Oauth2CredentialsSignalingAuthorization authorization;
    private RSAKey signingKey;

    @BeforeEach
    void setUp() throws JOSEException {
        authorization = new Oauth2CredentialsSignalingAuthorization(oauth2Client, tokenValidationService);
        signingKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    @Test
    void getType_shouldReturnOauth2ClientCredentials() {
        assertThat(authorization.getType()).isEqualTo("oauth2_client_credentials");
    }

    @Nested
    class IsAuthorized {

        @Test
        void shouldReturnSuccess_whenValidJwtWithSubClaim() {
            var callerId = "test-caller-id";
            var token = "token";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", callerId).build();
            when(tokenValidationService.validate(any(), any())).thenReturn(Result.success(claimToken));

            var result = authorization.isAuthorized(header -> "Bearer " + token);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isEqualTo(callerId);
            verify(tokenValidationService).validate(eq(token), any());
        }

        @Test
        void shouldReturnFailure_whenSubClaimIsNotThere() {
            var token = "token";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", null).build();
            when(tokenValidationService.validate(any(), any())).thenReturn(Result.success(claimToken));

            var result = authorization.isAuthorized(header -> "Bearer " + token);

            assertThat(result.failed()).isTrue();
        }

        @Test
        void shouldReturnFailure_whenTokenValidationFails() {
            when(tokenValidationService.validate(any(), any())).thenReturn(Result.failure("validation error"));

            var result = authorization.isAuthorized(header -> "Bearer token");

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("validation error");
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsNull() {
            var result = authorization.isAuthorized(header -> null);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Authorization"));
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsBlank() {
            var result = authorization.isAuthorized(header -> "   ");

            assertThat(result.failed()).isTrue();
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsTooShort() {
            var result = authorization.isAuthorized(header -> "Bearer");

            assertThat(result.failed()).isTrue();
        }

    }

    @Nested
    class Evaluate {

        @Test
        void shouldReturnAuthorizationHeader_whenTokenRequestSucceeds() {
            var accessToken = "access-token-value";
            when(oauth2Client.requestToken(any()))
                    .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(accessToken).build()));

            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of(
                    "tokenEndpoint", "https://auth.example.com/token",
                    "clientId", "my-client-id",
                    "clientSecret", "my-client-secret"
            ));

            var result = authorization.evaluate(profile);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent().key()).isEqualTo("Authorization");
            assertThat(result.getContent().value()).isEqualTo("Bearer " + accessToken);
        }

        @Test
        void shouldFail_whenTokenRequestFails() {
            when(oauth2Client.requestToken(any()))
                    .thenReturn(Result.failure("Token endpoint unavailable"));

            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of(
                    "tokenEndpoint", "https://auth.example.com/token",
                    "clientId", "my-client-id",
                    "clientSecret", "my-client-secret"
            ));

            var result = authorization.evaluate(profile);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Token endpoint unavailable"));
        }

        @Test
        void shouldPassClientCredentialsToOauth2Client() {
            when(oauth2Client.requestToken(any()))
                    .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));

            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of(
                    "tokenEndpoint", "https://auth.example.com/token",
                    "clientId", "my-client-id",
                    "clientSecret", "my-client-secret"
            ));

            authorization.evaluate(profile);

            verify(oauth2Client).requestToken(any());
        }
    }

}
