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

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_CACHE_REFRESH_TIMEOUT;
import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_RATE_LIMIT_MIN_INTERVAL;
import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_REFRESH_AHEAD_TIME;

/**
 * A {@link PublicKeyResolver} that resolves a JSON Web Key Set from a URL and parses the JWK with the given ID
 */
public class JwksPublicKeyResolver implements PublicKeyResolver {
    private final Monitor monitor;
    private final KeyParserRegistry keyParserRegistry;
    private final JWKSource<SecurityContext> jwkSource;

    private JwksPublicKeyResolver(KeyParserRegistry keyParserRegistry, Monitor monitor, JWKSource<SecurityContext> jwkSource) {
        this.keyParserRegistry = keyParserRegistry;
        this.monitor = monitor;
        this.jwkSource = jwkSource;
    }

    /**
     * Creates a new resolver that does use any cache. That means, that every request hits the server.
     *
     * @param keyParserRegistry Should contain all relevant key parsers. The minimum recommendation is adding a {@code JwkParser}.
     * @param jwksUrl           The URL of the public key server, where a JWK Set can be obtained.
     * @param monitor           A monitor
     * @throws EdcException if the jwksUrl is malformed
     */
    public static JwksPublicKeyResolver create(KeyParserRegistry keyParserRegistry, String jwksUrl, Monitor monitor) {
        return create(keyParserRegistry, jwksUrl, monitor, 0);
    }

    /**
     * Creates a new resolver that does use any cache. That means, that every request hits the server.
     *
     * @param keyParserRegistry Should contain all relevant key parsers. The minimum recommendation is adding a {@code JwkParser}.
     * @param jwksUrl           The URL of the public key server, where a JWK Set can be obtained.
     * @param monitor           A monitor
     * @param cacheValidityMs   The time in milliseconds that public keys may be cached locally.
     * @throws EdcException if the jwksUrl is malformed
     */
    public static JwksPublicKeyResolver create(KeyParserRegistry keyParserRegistry, String jwksUrl, Monitor monitor, long cacheValidityMs) {

        try {
            var builder = JWKSourceBuilder.create(URI.create(jwksUrl).toURL()).retrying(false);
            if (cacheValidityMs > 0) {
                builder.cache(cacheValidityMs, DEFAULT_CACHE_REFRESH_TIMEOUT);

                // rate-limit must be < cache TTL, this would cause the cache to be refreshed more often than allowed
                if (cacheValidityMs < DEFAULT_RATE_LIMIT_MIN_INTERVAL) {
                    builder.rateLimited(cacheValidityMs - 1);
                }
                // cache TTL must be > refresh-ahead time plus refresh timeout
                if (cacheValidityMs < DEFAULT_REFRESH_AHEAD_TIME + DEFAULT_CACHE_REFRESH_TIMEOUT) {
                    builder.refreshAheadCache(false);
                }

            } else {
                // disable all optimizations
                builder.cache(false);
                builder.rateLimited(false);
                builder.refreshAheadCache(false);
            }
            var jwkSource = builder.build();
            return new JwksPublicKeyResolver(keyParserRegistry, monitor, jwkSource);

        } catch (MalformedURLException e) {
            monitor.warning("Malformed JWK URL: " + jwksUrl, e);
            throw new EdcException(e);
        }
    }

    @Override
    public Result<PublicKey> resolveKey(@Nullable String keyId) {
        var matcher = Optional.ofNullable(keyId) // get matcher with optional keyID property
                .map(kid -> new JWKMatcher.Builder().keyID(kid).build())
                .orElseGet(() -> new JWKMatcher.Builder().build());
        var selector = new JWKSelector(matcher);
        List<JWK> keys;
        try {
            keys = jwkSource.get(selector, new SimpleSecurityContext());
        } catch (KeySourceException e) {
            monitor.warning("Error while retrieving JWKSet", e);
            return Result.failure("Error while retrieving JWKSet: " + e.getMessage());
        }

        if (keys.isEmpty()) {
            var msg = "JWKSet did not contain a matching key (desired keyId: '%s')".formatted(keyId);
            monitor.warning(msg);
            return Result.failure(msg);
        }
        if (keys.size() > 1) {
            String msg = keyId == null ?
                    "JWKSet contained %d keys, but no keyId was specified. Please consider specifying a keyId.".formatted(keys.size()) :
                    "JWKSet contained %d matching keys (desired keyId: '%s'), where only 1 is expected. Will abort!".formatted(keys.size(), keyId);
            monitor.warning(msg);
            return Result.failure(msg);
        }

        var jwk = keys.get(0);

        return keyParserRegistry.parse(jwk.toJSONString()).map(k -> (PublicKey) k);
    }
}
