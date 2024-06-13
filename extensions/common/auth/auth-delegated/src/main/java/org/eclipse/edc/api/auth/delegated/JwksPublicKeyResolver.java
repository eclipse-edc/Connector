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
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

/**
 * A {@link PublicKeyResolver} that resolves a JSON Web Key Set from a URL and parses the JWK with the given ID
 */
public class JwksPublicKeyResolver implements PublicKeyResolver {
    private final String jwksUrl;
    private final Monitor monitor;
    private final KeyParserRegistry keyParserRegistry;

    public JwksPublicKeyResolver(KeyParserRegistry keyParserRegistry, String jwksUrl, Monitor monitor) {
        this.jwksUrl = jwksUrl;
        this.monitor = monitor;
        this.keyParserRegistry = keyParserRegistry;
    }

    @Override
    public Result<PublicKey> resolveKey(@Nullable String keyId) {
        var matcher = Optional.ofNullable(keyId) // get matcher with optional keyID property
                .map(kid -> new JWKMatcher.Builder().keyID(kid).build())
                .orElseGet(() -> new JWKMatcher.Builder().build());
        var selector = new JWKSelector(matcher);
        List<JWK> keys;
        try {
            // Nimbus has extensive properties for advanced JWKS retrieval, but we won't use them, as caching keys should be handled
            // in layers above.
            // c.f.: https://connect2id.com/products/nimbus-jose-jwt/examples/enhanced-jwk-retrieval

            var jwkSource = JWKSourceBuilder.create(URI.create(jwksUrl).toURL()).build();
            keys = jwkSource.get(selector, new SimpleSecurityContext());
        } catch (KeySourceException e) {
            monitor.warning("Error while retrieving JWKSet", e);
            return Result.failure("Error while retrieving JWKSet: " + e.getMessage());
        } catch (MalformedURLException e) {
            monitor.warning("Malformed JWK URL: " + jwksUrl, e);
            return Result.failure("Malformed JWK URL: " + jwksUrl);
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
