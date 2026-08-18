/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2.tokenexchange.logic;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.IssuerEqualsValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Oauth2TokenExchangeSignalingAuthorizationTest {

    private static final String TYPE = "oauth2_token_exchange";
    private static final String ENDPOINT = "https://broker.example.com/token";
    private static final String ISSUER = "https://broker.example.com";
    private static final String JWKS_URI = "https://broker.example.com/.well-known/jwks.json";
    private static final String RESOURCE = "urn:principal:consumer-controlplane";
    private static final String AUDIENCE = "profile-audience";
    private static final String DEFAULT_AUDIENCE = "configured-audience";
    private static final String DEFAULT_SCOPE = "configured-scope";
    private static final String CALLER_ID = "provider-data-plane";

    private final Oauth2Client oauth2Client = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry = mock();
    private final KeyParserRegistry keyParserRegistry = mock();
    private final Monitor monitor = mock();
    private final AtomicInteger workloadCredentialReads = new AtomicInteger();

    private static Map.Entry<String, Object> prop(String key, Object value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private Oauth2TokenExchangeSignalingAuthorization authorization() {
        return authorization(DEFAULT_AUDIENCE, DEFAULT_SCOPE);
    }

    private Oauth2TokenExchangeSignalingAuthorization authorization(String defaultAudience, String defaultScope) {
        return new Oauth2TokenExchangeSignalingAuthorization(oauth2Client, tokenValidationService,
                tokenValidationRulesRegistry, keyParserRegistry,
                () -> {
                    workloadCredentialReads.incrementAndGet();
                    return Result.success("workload-credential");
                },
                monitor, defaultAudience, defaultScope);
    }

    @SafeVarargs
    private AuthorizationProfile profile(Map.Entry<String, Object>... overrides) {
        var properties = new HashMap<String, Object>(Map.of(
                "type", TYPE,
                "tokenExchangeEndpoint", ENDPOINT,
                "issuer", ISSUER,
                "jwksUri", JWKS_URI,
                "resource", RESOURCE,
                "audience", AUDIENCE
        ));
        for (var override : overrides) {
            if (override.getValue() == null) {
                properties.remove(override.getKey());
            } else {
                properties.put(override.getKey(), override.getValue());
            }
        }
        return new AuthorizationProfile(TYPE, properties);
    }

    private void stubExchange(String token) {
        when(oauth2Client.requestToken(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(token).build()));
    }

    @Test
    void getType_shouldReturnOauth2TokenExchange() {
        assertThat(authorization().getType()).isEqualTo(TYPE);
    }

    @Nested
    class Evaluate {

        @Test
        void shouldReturnBearerHeaderAndSendTokenExchangeParameters() {
            stubExchange("exchanged-jwt");

            var result = authorization().evaluate(profile());

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent().key()).isEqualTo("Authorization");
            assertThat(result.getContent().value()).isEqualTo("Bearer exchanged-jwt");

            var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
            verify(oauth2Client).requestToken(captor.capture());
            var request = captor.getValue();
            assertThat(request.getUrl()).isEqualTo(ENDPOINT);
            assertThat(request.getParams())
                    .containsEntry("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                    .containsEntry("subject_token", "workload-credential")
                    .containsEntry("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
                    .containsEntry("requested_token_type", "urn:ietf:params:oauth:token-type:jwt")
                    .containsEntry("resource", RESOURCE)
                    .containsEntry("audience", AUDIENCE)
                    .containsEntry("scope", DEFAULT_SCOPE)
                    .doesNotContainKeys("client_id", "client_secret");
        }

        @Test
        void shouldPreferProfileAudienceOverConfiguredDefault() {
            stubExchange("exchanged-jwt");

            authorization().evaluate(profile(prop("audience", "profile-only-audience")));

            var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
            verify(oauth2Client).requestToken(captor.capture());
            assertThat(captor.getValue().getParams()).containsEntry("audience", "profile-only-audience");
        }

        @Test
        void shouldFallBackToConfiguredAudience_whenProfileHasNone() {
            stubExchange("exchanged-jwt");

            var result = authorization().evaluate(profile(prop("audience", null)));

            assertThat(result.succeeded()).isTrue();
            var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
            verify(oauth2Client).requestToken(captor.capture());
            assertThat(captor.getValue().getParams()).containsEntry("audience", DEFAULT_AUDIENCE);
        }

        @Test
        void shouldPreferProfileScopeOverDefault() {
            stubExchange("exchanged-jwt");

            authorization().evaluate(profile(prop("scope", "profile-scope")));

            var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
            verify(oauth2Client).requestToken(captor.capture());
            assertThat(captor.getValue().getScope()).isEqualTo("profile-scope");
        }

        @ParameterizedTest
        @ValueSource(strings = {"tokenExchangeEndpoint", "resource"})
        void shouldFail_whenMandatoryPropertyIsMissing(String property) {
            var result = authorization().evaluate(profile(prop(property, null)));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains(property);
            verifyNoInteractions(oauth2Client);
        }

        @Test
        void shouldFail_whenScopeIsNeitherOnProfileNorConfigured() {
            var result = authorization(DEFAULT_AUDIENCE, null).evaluate(profile());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("scope");
            verifyNoInteractions(oauth2Client);
        }

        @Test
        void shouldFail_whenWorkloadCredentialCannotBeRead() {
            var authorization = new Oauth2TokenExchangeSignalingAuthorization(oauth2Client, tokenValidationService,
                    tokenValidationRulesRegistry, keyParserRegistry, () -> Result.failure("no such file"),
                    monitor, DEFAULT_AUDIENCE, DEFAULT_SCOPE);

            var result = authorization.evaluate(profile());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("no such file");
            verifyNoInteractions(oauth2Client);
        }

        @Test
        void shouldFail_whenExchangeFails() {
            when(oauth2Client.requestToken(any())).thenReturn(Result.failure("broker unavailable"));

            var result = authorization().evaluate(profile());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("broker unavailable");
        }

        @Test
        void shouldExchangeOnEveryEvaluation() {
            stubExchange("exchanged-jwt");
            var authorization = authorization();

            authorization.evaluate(profile());
            authorization.evaluate(profile());

            verify(oauth2Client, times(2)).requestToken(any());
            assertThat(workloadCredentialReads).hasValue(2);
        }
    }

    @Nested
    class IsAuthorized {

        @Test
        void shouldReturnClientIdClaim_whenPresent() {
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());
            when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.success(ClaimToken.Builder.newInstance()
                            .claim("client_id", CALLER_ID)
                            .claim("sub", RESOURCE)
                            .build()));

            var result = authorization().isAuthorized(header -> "Bearer a-token", profile());

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isEqualTo(CALLER_ID);
            verify(tokenValidationService).validate(anyString(), any(PublicKeyResolver.class), anyList());
        }

        @Test
        void shouldFallBackToSubClaim_whenClientIdIsAbsent() {
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());
            when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("sub", CALLER_ID).build()));

            var result = authorization().isAuthorized(header -> "Bearer a-token", profile());

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isEqualTo(CALLER_ID);
        }

        @Test
        void shouldAddIssuerRule_andNotValidateAudience() {
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of(mock(TokenValidationRule.class)));
            when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("sub", "a-sub").build()));

            authorization().isAuthorized(header -> "Bearer a-token", profile());

            assertThat(capturedRules())
                    .hasSize(2)
                    .contains(new IssuerEqualsValidationRule(ISSUER))
                    .noneMatch(AudienceValidationRule.class::isInstance);
        }

        @Test
        void shouldFail_whenTokenValidationFails() {
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());
            when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.failure("'iss' claim does not match expected issuer"));

            var result = authorization().isAuthorized(header -> "Bearer a-token", profile());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("iss");
        }

        @Test
        void shouldFail_whenNeitherClientIdNorSubIsPresent() {
            when(tokenValidationRulesRegistry.getRules(any())).thenReturn(List.of());
            when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                    .thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

            var result = authorization().isAuthorized(header -> "Bearer a-token", profile());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("client_id").contains("sub");
        }

        @Test
        void shouldFail_whenJwksUriIsMissing() {
            var result = authorization().isAuthorized(header -> "Bearer a-token", profile(prop("jwksUri", null)));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("jwksUri");
            verifyNoInteractions(tokenValidationService);
        }

        @Test
        void shouldFail_whenInlineJwksIsProvided() {
            var result = authorization().isAuthorized(header -> "Bearer a-token",
                    profile(prop("jwks", Map.of("keys", List.of()))));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("Inline 'jwks' is not permitted");
            verifyNoInteractions(tokenValidationService);
        }

        @Test
        void shouldFail_whenIssuerIsMissing() {
            var result = authorization().isAuthorized(header -> "Bearer a-token", profile(prop("issuer", null)));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("issuer");
            verifyNoInteractions(tokenValidationService);
        }

        @Test
        void shouldFail_whenJwksUriIsMalformed() {
            var result = authorization().isAuthorized(header -> "Bearer a-token",
                    profile(prop("jwksUri", "not a valid url")));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("not a valid url");
            verifyNoInteractions(tokenValidationService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "Bearer", "Basic dXNlcjpwYXNz"})
        void shouldFail_whenAuthorizationHeaderIsNotBearer(String header) {
            var result = authorization().isAuthorized(name -> header, profile());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("Authorization");
        }

        @Test
        void shouldFail_whenAuthorizationHeaderIsAbsent() {
            var result = authorization().isAuthorized(name -> null, profile());

            assertThat(result.failed()).isTrue();
        }

        private List<TokenValidationRule> capturedRules() {
            var captor = ArgumentCaptor.forClass(List.class);
            verify(tokenValidationService).validate(anyString(), any(PublicKeyResolver.class), captor.capture());
            return captor.getValue();
        }
    }
}
