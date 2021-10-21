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

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolves public signing certificates for the identity provider. Used to verify JWTs.
 * This implementation supports key rotation and refresh by calling {@link #refreshKeys()} on a periodic basis.
 */
public class IdentityProviderKeyResolver implements PublicKeyResolver, Runnable {
    private final String jwksUrl;
    private final Monitor monitor;

    private final ObjectMapper mapper;
    private final AtomicReference<Map<String, RSAPublicKey>> cache = new AtomicReference<>(); // the current key cache, atomic for thread-safety
    private final HttpClient httpClient;

    /**
     * Ctor.
     *
     * @param jwksUrl the URL specified by 'jwks_uri' in the document returned by the identity provider's metadata endpoint.
     * @param httpClient the HttpClient
     */
    public IdentityProviderKeyResolver(String jwksUrl, Monitor monitor, HttpClient httpClient) {
        this.jwksUrl = jwksUrl;
        this.monitor = monitor;
        this.httpClient = httpClient;
        mapper = new ObjectMapper();
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
            var request = HttpRequest.newBuilder(new URI(jwksUrl)).GET().build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                monitor.severe("Unable to refresh identity provider keys. Response code was: " + response.statusCode());
                return;
            }

            var body = response.body();
            if (body == null) {
                monitor.severe("Unable to refresh identity provider keys. An empty response was returned.");
                return;
            }

            var jwkKeys = mapper.readValue(body, JwkKeys.class);
            var keys = jwkKeys.getKeys();
            if (keys == null || keys.isEmpty()) {
                monitor.severe("No keys returned from identity provider.");
                return;
            }

            var newKeys = deserializeKeys(keys);
            if (newKeys == null) {
                return;
            }
            cache.set(newKeys);

        } catch (IOException | InterruptedException e) {
            monitor.severe("Error resolving identity provider keys: " + jwksUrl, e);
        } catch (URISyntaxException e) {
            monitor.severe("Error parsing URI: " + jwksUrl, e);
        }
    }

    /**
     * Deserializes JWK keys into RSA public keys. Accessible for testing.
     */
    Map<String, RSAPublicKey> deserializeKeys(List<JwkKey> jwkKeys) {
        Map<String, RSAPublicKey> newKeys = new HashMap<>();

        // instantiate the RSA public keys by kid
        for (JwkKey key : jwkKeys) {
            if (!"RSA".equals(key.getKty()) || !"sig".equals(key.getUse())) {
                continue;
            }
            BigInteger modulus = unsignedInt(key.getN());
            BigInteger exponent = unsignedInt(key.getE());
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);
                newKeys.put(key.getKid(), publicKey);
            } catch (GeneralSecurityException e) {
                monitor.severe("Error parsing identity provider public key, skipping. The kid is: " + key.getKid());
            }
        }
        return newKeys;
    }

    private BigInteger unsignedInt(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

}
