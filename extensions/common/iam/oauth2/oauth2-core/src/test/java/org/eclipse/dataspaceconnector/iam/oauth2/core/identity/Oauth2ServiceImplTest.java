/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.identity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.core.jwt.JwtDecoratorRegistryImpl;
import org.eclipse.dataspaceconnector.core.jwt.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.Oauth2ValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.NOT_BEFORE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;
import static org.mockserver.stop.Stop.stopQuietly;

class Oauth2ServiceImplTest {

    private static final String CLIENT_ID = "client-test";
    private static final String PRIVATE_KEY_ALIAS = "pk-test";
    private static final String PUBLIC_CERTIFICATE_ALIAS = "cert-test";
    private static final String PROVIDER_AUDIENCE = "audience-test";
    private static final int OAUTH2_SERVER_PORT = getFreePort();
    private static final String OAUTH2_SERVER_URL = "http://localhost:" + OAUTH2_SERVER_PORT;

    private final Instant now = Instant.now();
    private final OkHttpClient okHttpClient = testOkHttpClient();
    private final TokenGenerationService tokenGenerationService = mock(TokenGenerationService.class);
    private final CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider = mock(CredentialsRequestAdditionalParametersProvider.class);
    private Oauth2ServiceImpl authService;
    private JWSSigner jwsSigner;
    private static ClientAndServer oauth2Server;

    @BeforeAll
    public static void startServer() {
        oauth2Server = ClientAndServer.startClientAndServer(OAUTH2_SERVER_PORT);
    }

    @AfterAll
    public static void stopServer() {
        stopQuietly(oauth2Server);
    }

    @BeforeEach
    void setUp() throws JOSEException {
        var testKey = testKey();

        jwsSigner = new RSASSASigner(testKey.toPrivateKey());
        var publicKeyResolverMock = mock(PublicKeyResolver.class);
        var privateKeyResolverMock = mock(PrivateKeyResolver.class);
        var certificateResolverMock = mock(CertificateResolver.class);
        when(publicKeyResolverMock.resolveKey(anyString())).thenReturn(testKey.toPublicKey());
        var configuration = Oauth2Configuration.Builder.newInstance()
                .tokenUrl(OAUTH2_SERVER_URL)
                .clientId(CLIENT_ID)
                .privateKeyAlias(PRIVATE_KEY_ALIAS)
                .publicCertificateAlias(PUBLIC_CERTIFICATE_ALIAS)
                .providerAudience(PROVIDER_AUDIENCE)
                .privateKeyResolver(privateKeyResolverMock)
                .certificateResolver(certificateResolverMock)
                .identityProviderKeyResolver(publicKeyResolverMock)
                .build();

        var clock = Clock.fixed(now, UTC);
        var validationRulesRegistry = new Oauth2ValidationRulesRegistryImpl(configuration, clock);
        var tokenValidationService = new TokenValidationServiceImpl(publicKeyResolverMock, validationRulesRegistry);

        authService = new Oauth2ServiceImpl(configuration, tokenGenerationService, okHttpClient,
                new JwtDecoratorRegistryImpl(), new TypeManager(), tokenValidationService,
                credentialsRequestAdditionalParametersProvider);
    }

    @Test
    void obtainClientCredentials() {
        when(credentialsRequestAdditionalParametersProvider.provide(any())).thenReturn(emptyMap());
        when(tokenGenerationService.generate(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        var clientCredentialsRequest = new HttpRequest().withBody(params(
                param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                param("grant_type", "client_credentials"),
                param("client_assertion", "token"),
                param("scope", "scope")
        ));
        var responseBody = Map.of("access_token", "accessToken");
        oauth2Server.when(clientCredentialsRequest, once()).respond(new HttpResponse().withStatusCode(200).withBody(json(responseBody)));
        var tokenParameters = TokenParameters.Builder.newInstance().audience("audience").scope("scope").build();

        var result = authService.obtainClientCredentials(tokenParameters);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("accessToken");
        oauth2Server.verify(clientCredentialsRequest);
    }

    @Test
    void obtainClientCredentials_addsAdditionalFormParameters() {
        when(credentialsRequestAdditionalParametersProvider.provide(any())).thenReturn(Map.of("parameterKey", "parameterValue"));
        when(tokenGenerationService.generate(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        var clientCredentialsRequest = new HttpRequest().withBody(params(
                param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                param("grant_type", "client_credentials"),
                param("client_assertion", "token"),
                param("scope", "scope"),
                param("parameterKey", "parameterValue")
        ));
        var responseBody = Map.of("access_token", "accessToken");
        oauth2Server.when(clientCredentialsRequest, once()).respond(new HttpResponse().withStatusCode(200).withBody(json(responseBody)));
        var tokenParameters = TokenParameters.Builder.newInstance().audience("audience").scope("scope").build();

        authService.obtainClientCredentials(tokenParameters);

        oauth2Server.verify(clientCredentialsRequest);
    }

    @Test
    void verifyNoAudienceToken() {
        var jwt = createJwt(null, Date.from(now.minusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, PROVIDER_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyInvalidAudienceToken() {
        var jwt = createJwt("different.audience", Date.from(now.minusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, PROVIDER_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyInvalidAttemptUseNotBeforeToken() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.plusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, PROVIDER_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyExpiredToken() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.minusSeconds(1000)), Date.from(now.minusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, PROVIDER_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyValidJwt() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.minusSeconds(1000)), new Date(System.currentTimeMillis() + 1000000));

        var result = authService.verifyJwtToken(jwt, PROVIDER_AUDIENCE);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims()).hasSize(3).containsKeys(AUDIENCE, NOT_BEFORE, EXPIRATION_TIME);
    }

    private RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    private TokenRepresentation createJwt(String aud, Date nbf, Date exp) {
        var claimsSet = new JWTClaimsSet.Builder()
                .audience(aud)
                .notBeforeTime(nbf)
                .expirationTime(exp).build();
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("an-id").build();

        try {
            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(jwsSigner);
            return TokenRepresentation.Builder.newInstance().token(jwt.serialize()).build();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }
}
