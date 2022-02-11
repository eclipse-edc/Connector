/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.JwkKey;
import org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.JwkKeys;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Resolves public signing certificates for the identity provider. Used to verify JWTs.
 * This implementation supports key rotation and refresh by calling {@link #refreshKeys()} on a periodic basis.
 */
public class IdentityProviderKeyResolver implements PublicKeyResolver, Runnable {
    private final String jwksUrl;
    private final Monitor monitor;

    private final ObjectMapper mapper;
    private final AtomicReference<Map<String, RSAPublicKey>> cache = new AtomicReference<>(); // the current key cache, atomic for thread-safety
    private final OkHttpClient httpClient;

    /**
     * Ctor.
     *
     * @param jwksUrl     the URL specified by 'jwks_uri' in the document returned by the identity provider's metadata endpoint.
     * @param typeManager the type manager
     */
    public IdentityProviderKeyResolver(String jwksUrl, Monitor monitor, OkHttpClient httpClient, TypeManager typeManager) {
        this.jwksUrl = jwksUrl;
        this.monitor = monitor;
        this.httpClient = httpClient;
        mapper = typeManager.getMapper();
    }

    @Override
    public RSAPublicKey resolveKey(String id) {
        return cache.get().get(id);
    }

    @Override
    public void run() {
        refreshKeys();
    }

    public void refreshKeys() {
        try {
            Request request = new Request.Builder().url(jwksUrl).get().build();

            Response response = httpClient.newCall(request).execute();
            if (response.code() != 200) {
                monitor.severe("Unable to refresh identity provider keys. Response code was: " + response.code());
                return;
            }
            try (var body = response.body()) {
                if (body == null) {
                    monitor.severe("Unable to refresh identity provider keys. An empty response was returned.");
                    return;
                }

                // deserialize the JWKs
                JwkKeys jwkKeys = mapper.readValue(body.string(), JwkKeys.class);
                List<JwkKey> keys = jwkKeys.getKeys();
                if (keys == null || keys.isEmpty()) {
                    monitor.severe("No keys returned from identity provider.");
                    return;
                }

                Map<String, RSAPublicKey> newKeys = deserializeKeys(keys);
                if (newKeys == null) {
                    return;
                }

                cache.set(newKeys);   // reset the cache
            }
        } catch (IOException e) {
            monitor.severe("Error resolving identity provider keys: " + jwksUrl, e);
        }
    }

    /**
     * Deserializes JWK keys into RSA public keys. Accessible for testing.
     */
    Map<String, RSAPublicKey> deserializeKeys(List<JwkKey> jwkKeys) {
        return jwkKeys.stream()
                .filter(new JwkKeyPredicate())
                .map(this::deserializeKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, RSAPublicKey> deserializeKey(JwkKey key) {
        BigInteger modulus = unsignedInt(key.getN());
        BigInteger exponent = unsignedInt(key.getE());
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return new AbstractMap.SimpleEntry<>(key.getKid(), (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec));
        } catch (GeneralSecurityException e) {
            monitor.severe("Error parsing identity provider public key, skipping. The kid is: " + key.getKid());
        }
        return null;
    }

    private BigInteger unsignedInt(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

    static final class JwkKeyPredicate implements Predicate<JwkKey> {
        @Override
        public boolean test(JwkKey key) {
            return "RSA".equals(key.getKty());
        }
    }
}
