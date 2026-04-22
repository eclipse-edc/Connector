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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Oauth2CredentialsSignalingAuthorizationTest {

    private final Oauth2Client oauth2Client = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry = mock();
    private final KeyParserRegistry keyParserRegistry = mock();
    private final Monitor monitor = mock();
    private final Oauth2CredentialsSignalingAuthorization authorization = new Oauth2CredentialsSignalingAuthorization(
            oauth2Client, tokenValidationService, tokenValidationRulesRegistry, keyParserRegistry, monitor);

    @Test
    void getType_shouldReturnOauth2ClientCredentials() {
        assertThat(authorization.getType()).isEqualTo("oauth2_client_credentials");
    }

    @Nested
    class IsAuthorized {

        @RegisterExtension
        static WireMockExtension jwksServer = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();

        @Test
        void shouldReturnSuccess_whenValidJwtWithSubClaim() {
            var callerId = "test-caller-id";
            var token = "token";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", callerId).build();
            when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.success(claimToken));
            var validationRules = List.of(mock(TokenValidationRule.class));
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(validationRules);
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> "Bearer " + token, profile);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isEqualTo(callerId);
            verify(tokenValidationService).validate(eq(token), any(), same(validationRules));
        }

        @Test
        void shouldReturnFailure_whenSubClaimIsNotThere() {
            var token = "token";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", null).build();
            when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.success(claimToken));
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> "Bearer " + token, profile);

            assertThat(result.failed()).isTrue();
        }

        @Test
        void shouldReturnFailure_whenTokenValidationFails() {
            when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.failure("validation error"));
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> "Bearer token", profile);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("validation error");
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsNull() {
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> null, profile);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Authorization"));
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsBlank() {
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> "   ", profile);

            assertThat(result.failed()).isTrue();
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsTooShort() {
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> "Bearer", profile);

            assertThat(result.failed()).isTrue();
        }

        @Test
        void shouldFail_whenJwksUriIsMalformed() {
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of("jwksUri", "not a valid url"));

            var result = authorization.isAuthorized(header -> "Bearer token", profile);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("not a valid url");
        }

        @Test
        void shouldFail_whenInlineJwksIsInvalid() {
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of("jwks", Map.of("invalid", "structure")));

            var result = authorization.isAuthorized(header -> "Bearer token", profile);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("Failed to parse inline jwks");
        }

        @Test
        void shouldSkipSignatureVerification_whenNoJwksConfigured() {
            var callerId = "test-caller-id";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", callerId).build();
            when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.success(claimToken));
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of());

            var result = authorization.isAuthorized(header -> "Bearer token", profile);

            assertThat(result.succeeded()).isTrue();
        }

        @Test
        void shouldReturnSuccess_whenJwksUriIsConfiguredAndKeyMatches() {
            var key = generateKey("test-key");
            var token = createToken(key);
            var callerId = "caller-from-jwks-uri";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", callerId).build();

            var jwksPath = "/.well-known/jwks.json";
            jwksServer.stubFor(get(urlEqualTo(jwksPath))
                    .willReturn(okJson(new JWKSet(key.toPublicJWK()).toString())));

            when(keyParserRegistry.parse(any())).thenReturn(Result.success(mock(PublicKey.class)));
            when(tokenValidationService.validate(eq(token), any(), anyList())).thenReturn(Result.success(claimToken));
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());

            var jwksUri = "http://localhost:%d%s".formatted(jwksServer.getPort(), jwksPath);
            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of("jwksUri", jwksUri));

            var result = authorization.isAuthorized(header -> "Bearer " + token, profile);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isEqualTo(callerId);
        }

        @Test
        void shouldReturnSuccess_whenInlineJwksIsConfiguredAndKeyMatches() {
            var key = generateKey("inline-key");
            var token = createToken(key);
            var callerId = "caller-from-inline-jwks";
            var claimToken = ClaimToken.Builder.newInstance().claim("sub", callerId).build();

            when(keyParserRegistry.parse(any())).thenReturn(Result.success(mock(PublicKey.class)));
            when(tokenValidationService.validate(eq(token), any(), anyList())).thenReturn(Result.success(claimToken));
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());

            var profile = new AuthorizationProfile("oauth2_client_credentials", Map.of("jwks", jwksMap(key.toPublicJWK())));

            var result = authorization.isAuthorized(header -> "Bearer " + token, profile);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isEqualTo(callerId);
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

    private JWK generateKey(String keyId) {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519).keyID(keyId).keyUse(KeyUse.SIGNATURE).generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String createToken(JWK key) {
        try {
            var signer = CryptoConverter.createSigner(key);
            var algorithm = CryptoConverter.getRecommendedAlgorithm(signer);
            var header = new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build();
            var claims = new JWTClaimsSet.Builder().subject("test-subject").build();
            var jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> jwksMap(JWK... keys) {
        return Map.of("keys", java.util.Arrays.stream(keys).map(JWK::toJSONObject).toList());
    }

}
