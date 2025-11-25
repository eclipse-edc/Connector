/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.virtualized.api.authentication;

import com.nimbusds.jose.jwk.JWKSet;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.Cache;

import java.net.URL;
import java.security.PublicKey;

public class JwksResolver implements PublicKeyResolver {
    private final Cache<URL, JWKSet> jwksCache;
    private final URL jwksUrl;
    private final KeyParserRegistry keyParserRegistry;

    public JwksResolver(URL jwksUrl, KeyParserRegistry keyParserRegistry, long cacheExpirationInMillis) {
        this.jwksUrl = jwksUrl;
        this.keyParserRegistry = keyParserRegistry;
        jwksCache = new Cache<>(this::loadJwks, cacheExpirationInMillis);
    }

    @Override
    public Result<PublicKey> resolveKey(String kid) {
        var jwks = jwksCache.get(jwksUrl);
        var jwk = jwks.getKeyByKeyId(kid);
        if (jwk == null) {
            return Result.failure("JWK not found for kid: " + kid);
        }
        return keyParserRegistry.parsePublic(jwk.toJSONString());
    }

    private JWKSet loadJwks(URL jwksUrl) {
        try {
            return JWKSet.load(jwksUrl);
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

}
