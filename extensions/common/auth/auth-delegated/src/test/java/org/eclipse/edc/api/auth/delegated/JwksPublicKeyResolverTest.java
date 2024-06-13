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
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

import java.util.Arrays;
import java.util.Map;

import static org.eclipse.edc.api.auth.delegated.TestFunctions.generateKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

@ComponentTest
class JwksPublicKeyResolverTest {

    private static ClientAndServer jwksServer;
    private final KeyParserRegistry keyParserRegistry = new KeyParserRegistryImpl();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Monitor monitor = mock();
    private JwksPublicKeyResolver resolver;

    @BeforeAll
    static void prepare() {
        jwksServer = ClientAndServer.startClientAndServer(getFreePort());
    }

    @AfterAll
    static void teardown() {
        stopQuietly(jwksServer);
    }

    @BeforeEach
    void setup() {
        jwksServer.reset();
        keyParserRegistry.register(new JwkParser(mapper, monitor));
        resolver = new JwksPublicKeyResolver(keyParserRegistry, jwksServerUrl(), monitor);
    }

    @Test
    void resolve_singleKey() {
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(generateKey("foo-bar-key").toPublicJWK())));

        assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
    }

    @Test
    void resolve_multipleUniqueKeys() {

        var key1 = generateKey("foo-bar-key1").toPublicJWK();
        var key2 = generateKey("foo-bar-key2").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1, key2)));

        assertThat(resolver.resolveKey("foo-bar-key2")).isSucceeded();
    }

    @Test
    void resolve_multipleKeysWithSameId() {

        var key1 = generateKey("foo-bar-keyX").toPublicJWK();
        var key2 = generateKey("foo-bar-keyX").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1, key2)));

        assertThat(resolver.resolveKey("foo-bar-keyX")).isFailed()
                .detail().isEqualTo("JWKSet contained 2 matching keys (desired keyId: 'foo-bar-keyX'), where only 1 is expected. Will abort!");
    }

    @Test
    void resolve_keyNotFound() {
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(generateKey("foo-bar-key").toPublicJWK())));

        assertThat(resolver.resolveKey("not-exist")).isFailed()
                .detail().isEqualTo("JWKSet did not contain a matching key (desired keyId: 'not-exist')");
    }

    @Test
    void resolve_multipleKeys_noKeyIdGiven() {
        var key1 = generateKey("foo-bar-key1").toPublicJWK();
        var key2 = generateKey("foo-bar-key2").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1, key2)));

        assertThat(resolver.resolveKey(null)).isFailed()
                .detail().isEqualTo("JWKSet contained 2 keys, but no keyId was specified. Please consider specifying a keyId.");
    }

    @Test
    void resolve_singleKey_noKeyId() {
        var key1 = generateKey("foo-bar-key1").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1)));

        assertThat(resolver.resolveKey(null)).isSucceeded();
    }


    @Test
    void resolve_malformedKeyUrl() {
        resolver = new JwksPublicKeyResolver(keyParserRegistry, "foobar://invalid.url", monitor);
        assertThat(resolver.resolveKey("test-key")).isFailed()
                .detail().isEqualTo("Malformed JWK URL: foobar://invalid.url");
    }

    @Test
    void resolve_invalidKeyUrl() {
        resolver = new JwksPublicKeyResolver(keyParserRegistry, "http:_invalid.url", monitor);
        assertThat(resolver.resolveKey("test-key")).isFailed()
                .detail().contains("Error while retrieving JWKSet");
    }

    private @NotNull String jwksServerUrl() {
        return "http://localhost:%d/.well-known/jwks.json".formatted(jwksServer.getPort());
    }

    private HttpRequest jwksRequest() {
        return request()
                .withPath("/.well-known/jwks.json");
    }

    private String jwksObject(JWK... keys) {
        var keyList = Arrays.stream(keys).map(JWK::toJSONObject).toList();
        var m = Map.of("keys", keyList);

        try {
            return mapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}