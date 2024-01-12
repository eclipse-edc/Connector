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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.iam.oauth2.identity;

import okhttp3.Request;
import org.eclipse.edc.iam.oauth2.jwt.JwkKey;
import org.eclipse.edc.iam.oauth2.jwt.JwkKeys;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Resolves public signing certificates for the identity provider. Used to verify JWTs.
 * The keys are cached and the resolver must be started calling the `start` method
 */
public class IdentityProviderKeyResolver implements PublicKeyResolver {
    private static final String RSA = "RSA";
    private final Monitor monitor;
    private final TypeManager typeManager;
    private final ScheduledExecutorService executorService;
    private final AtomicReference<Map<String, RSAPublicKey>> cache = new AtomicReference<>(emptyMap()); // the current key cache, atomic for thread-safety
    private final EdcHttpClient httpClient;
    private final String jwksUrl;
    private final int keyRefreshInterval;
    private final Predicate<JwkKey> isRsa = key -> RSA.equals(key.getKty());

    public IdentityProviderKeyResolver(Monitor monitor, EdcHttpClient httpClient, TypeManager typeManager, String jwksUrl, int keyRefreshInterval) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.jwksUrl = jwksUrl;
        this.keyRefreshInterval = keyRefreshInterval;
    }

    @Override
    public Result<PublicKey> resolveKey(String id) {
        return Result.success(cache.get().get(id));
    }

    /**
     * Start the keys cache refreshing job.
     * Throws exception if it's not able to load the cache at startup.
     */
    public void start() {
        var result = refreshKeys();
        if (result.failed()) {
            throw new EdcException(String.format("Failed to get keys from %s: %s", jwksUrl, String.join(", " + result.getFailureMessages())));
        }

        executorService.scheduleWithFixedDelay(this::refreshKeys, keyRefreshInterval, keyRefreshInterval, MINUTES);
    }

    /**
     * Stops the cache refresh job.
     */
    public void stop() {
        executorService.shutdownNow();
    }

    /**
     * Get keys from the JWKS provider. Protected for testing purposes.
     *
     * @return succeed if keys are retrieved correctly, failure otherwise
     */
    protected Result<Map<String, RSAPublicKey>> getKeys() {
        var request = new Request.Builder().url(jwksUrl).get().build();
        try (var response = httpClient.execute(request)) {
            if (response.code() == 200) {
                var body = response.body();
                if (body == null) {
                    var message = "Unable to refresh identity provider keys. An empty response was returned.";
                    monitor.severe(message);
                    return Result.failure(message);
                }

                var jwsKeys = typeManager.readValue(body.string(), JwkKeys.class);
                var keys = jwsKeys.getKeys();
                if (keys == null || keys.isEmpty()) {
                    var message = "No keys returned from identity provider.";
                    monitor.warning(message);
                    return Result.failure(message);
                }

                return Result.success(deserializeKeys(keys));

            } else {
                var message = "Unable to refresh identity provider keys. Response code was: " + response.code();
                monitor.severe(message);
                return Result.failure(message);
            }
        } catch (Exception e) {
            var message = "Error resolving identity provider keys: " + jwksUrl;
            monitor.severe(message, e);
            return Result.failure(message);
        }
    }

    private Result<Void> refreshKeys() {
        var result = getKeys();
        if (result.succeeded()) {
            cache.set(result.getContent());
        }
        return result.map(it -> null);
    }

    private Map<String, RSAPublicKey> deserializeKeys(List<JwkKey> jwkKeys) {
        return jwkKeys.stream()
                .filter(isRsa)
                .map(this::deserializeKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, RSAPublicKey> deserializeKey(JwkKey key) {
        var modulus = unsignedInt(key.getN());
        var exponent = unsignedInt(key.getE());
        var rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        try {
            var keyFactory = KeyFactory.getInstance(RSA);
            return new AbstractMap.SimpleEntry<>(key.getKid(), (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec));
        } catch (GeneralSecurityException e) {
            monitor.severe("Error parsing identity provider public key, skipping. The kid is: " + key.getKid());
        }
        return null;
    }

    private BigInteger unsignedInt(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

}
