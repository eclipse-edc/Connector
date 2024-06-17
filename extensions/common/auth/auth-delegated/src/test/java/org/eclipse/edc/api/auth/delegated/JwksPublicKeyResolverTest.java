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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_CACHE_TIME_TO_LIVE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.api.auth.delegated.TestFunctions.generateKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.mockserver.verify.VerificationTimes.never;

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
        resolver = JwksPublicKeyResolver.create(keyParserRegistry, jwksServerUrl(), monitor, DEFAULT_CACHE_TIME_TO_LIVE);
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
        jwksServer.verify(jwksRequest(), exactly(1));
    }

    @Test
    void resolve_multipleKeysWithSameId() {

        var key1 = generateKey("foo-bar-keyX").toPublicJWK();
        var key2 = generateKey("foo-bar-keyX").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1, key2)));

        assertThat(resolver.resolveKey("foo-bar-keyX")).isFailed()
                .detail().isEqualTo("JWKSet contained 2 matching keys (desired keyId: 'foo-bar-keyX'), where only 1 is expected. Will abort!");
        jwksServer.verify(jwksRequest(), exactly(1));

    }

    @Test
    void resolve_keyNotFound() {
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(generateKey("foo-bar-key").toPublicJWK())));

        assertThat(resolver.resolveKey("not-exist")).isFailed()
                .detail().isEqualTo("JWKSet did not contain a matching key (desired keyId: 'not-exist')");
        // the JWK source has this weird behaviour where it tries again when no key matches the selector.
        // ref: JWKSetBasedJWKSource.java
        jwksServer.verify(jwksRequest(), exactly(2));

    }

    @Test
    void resolve_multipleKeys_noKeyIdGiven() {
        var key1 = generateKey("foo-bar-key1").toPublicJWK();
        var key2 = generateKey("foo-bar-key2").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1, key2)));

        assertThat(resolver.resolveKey(null)).isFailed()
                .detail().isEqualTo("JWKSet contained 2 keys, but no keyId was specified. Please consider specifying a keyId.");
        jwksServer.verify(jwksRequest(), exactly(1));
    }

    @Test
    void resolve_singleKey_noKeyId() {
        var key1 = generateKey("foo-bar-key1").toPublicJWK();
        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(key1)));

        assertThat(resolver.resolveKey(null)).isSucceeded();
        jwksServer.verify(jwksRequest(), exactly(1));
    }


    @Test
    void resolve_malformedKeyUrl() {

        assertThatThrownBy(() -> JwksPublicKeyResolver.create(keyParserRegistry, "foobar://invalid.url", monitor, DEFAULT_CACHE_TIME_TO_LIVE))
                .isInstanceOf(EdcException.class)
                .hasRootCauseInstanceOf(MalformedURLException.class);

        verify(monitor).warning(contains("Malformed JWK URL: foobar://invalid.url"), isA(MalformedURLException.class));
    }

    @Test
    void resolve_invalidKeyUrl() {
        resolver = JwksPublicKeyResolver.create(keyParserRegistry, "http:_invalid.url", monitor, DEFAULT_CACHE_TIME_TO_LIVE);
        assertThat(resolver.resolveKey("test-key")).isFailed()
                .detail().contains("Error while retrieving JWKSet");
        jwksServer.verify(jwksRequest(), never());

    }

    @Test
    void resolve_verifyHitsCache() {
        var cacheTtl = 1000;
        resolver = JwksPublicKeyResolver.create(keyParserRegistry, jwksServerUrl(), monitor, cacheTtl);

        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(generateKey("foo-bar-key").toPublicJWK())));

        assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
        assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
        jwksServer.verify(jwksRequest(), exactly(1));

        // now wait for the cache to expire, try again and assert that the key server is hit
        await().atMost(Duration.ofMillis(3 * cacheTtl))
                .untilAsserted(() -> {
                    assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
                    jwksServer.verify(jwksRequest(), exactly(1));
                });

    }

    @Test
    void resolve_verifyNoHitsCache() {
        resolver = JwksPublicKeyResolver.create(keyParserRegistry, jwksServerUrl(), monitor);

        jwksServer.when(jwksRequest())
                .respond(response().withStatusCode(200).withBody(jwksObject(generateKey("foo-bar-key").toPublicJWK())));

        assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
        assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
        assertThat(resolver.resolveKey("foo-bar-key")).isSucceeded();
        jwksServer.verify(jwksRequest(), exactly(3));
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