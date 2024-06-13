/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.auth.delegated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

@ComponentTest
class DelegatedAuthenticationServiceTest {

    private static final long TEST_CACHE_VALIDITY = 50;
    private static ClientAndServer keyServer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void prepare() {
        keyServer = ClientAndServer.startClientAndServer(getFreePort());

    }

    @AfterAll
    static void teardown() {
        stopQuietly(keyServer);
    }

    @BeforeEach
    void setUp() {
        keyServer.reset();
    }

    @Test
    void isAuthenticated_jwks_singleKey() throws JOSEException {
        var port = keyServer.getLocalPort();
        var authService = new DelegatedAuthenticationService("http://localhost:%d/.well-known/jwks.json".formatted(port), TEST_CACHE_VALIDITY, mock());

        var key = new OctetKeyPairGenerator(Curve.Ed25519).keyID("test-key").keyUse(KeyUse.SIGNATURE).generate();
        keyServer.when(jwksRequest()).respond(response().withStatusCode(200).withBody(jkwsObject(key)));

        var token = createToken(key);
        var headers = Map.of("Authorization", List.of("Bearer " + token));

        assertThat(authService.isAuthenticated(headers)).isTrue();
        keyServer.verify(jwksRequest(), VerificationTimes.exactly(1));
    }

    @Test
    void isAuthenticated_jwks_multipleKeys() throws JOSEException {
        var port = keyServer.getLocalPort();
        var authService = new DelegatedAuthenticationService("http://localhost:%d/.well-known/jwks.json".formatted(port), TEST_CACHE_VALIDITY, mock());

        var key1 = new OctetKeyPairGenerator(Curve.Ed25519).keyID("test-key1").generate();
        var key2 = new ECKeyGenerator(Curve.P_256).keyID("test-key2").generate();
        keyServer.when(jwksRequest()).respond(response().withStatusCode(200).withBody(jkwsObject(key1, key2)));

        var token = createToken(key1);
        var headers = Map.of("Authorization", List.of("Bearer " + token));

        assertThat(authService.isAuthenticated(headers)).isTrue();

    }

    @Test
    void isAuthenticated_jwks_keyNotFound() {

    }

    @Test
    void isAuthenticated_jwks_multiKeys_noKeyId() {

    }

    @Test
    void isAuthenticated_jwks_singleKey_noKeyId() {
    }

    @Test
    void isAuthenticated_pem() {

    }

    @Test
    void isAuthenticated_invalidKeyUrl() {

    }

    @Test
    void isAuthenticated_tokenNotValid() {

    }

    @Test
    void isAuthenticated_nbfInvalid() {

    }

    @Test
    void isAuthenticated_expInvalid() {

    }

    @Test
    void isAuthenticated_valid() {

    }

    @Test
    void isAuthenticated_withNbfAndExp_valid() {
    }

    @Test
    void isAuthenticated_multipleAuthHeaders_shouldReject() {

    }

    private String createToken(JWK key) {
        var signer = CryptoConverter.createSigner(key);
        var algorithm = CryptoConverter.getRecommendedAlgorithm(signer);

        var header = new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build();
        var claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .issuer("test-issuer")
                .subject("test-subject")
                .build();

        var jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest jwksRequest() {
        return request()
                .withPath("/.well-known/jwks.json");
    }

    private HttpRequest pemRequest() {
        return request()
                .withPath("/.well-known/pem");
    }

    private String jkwsObject(JWK... keys) {
        var keyList = Arrays.stream(keys).map(JWK::toJSONObject).toList();
        var m = Map.of("keys", keyList);

        try {
            return mapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}