/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class VaultApi {

    private final String vaultUrl;
    private final String token;

    private final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VaultApi(String vaultUrl, String token) {
        this.vaultUrl = vaultUrl;
        this.token = token;
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public String getToken() {
        return token;
    }


    public void enableTransitEngine() {
        // activate transit secrets engine
        var payload = """
                {"type":"transit"}
                """;
        var rq = new Request.Builder()
                .url("%s/v1/sys/mounts/transit".formatted(vaultUrl))
                .header("X-Vault-Token", token)
                .post(RequestBody.create(payload.getBytes(), MediaType.parse("application/json")))
                .build();
        try (var response = httpClient.newCall(rq).execute()) {
            assertThat(response.isSuccessful()).describedAs(response.message()).isTrue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JWK addTransitEngineKey(String keyName, String keyId) {
        var payload = """
                {"type":"ed25519"}
                """;
        var rq = new Request.Builder()
                .url("%s/v1/transit/keys/%s".formatted(vaultUrl, keyName))
                .header("X-Vault-Token", token)
                .post(RequestBody.create(payload.getBytes(), MediaType.parse("application/json")))
                .build();
        try (var response = httpClient.newCall(rq).execute()) {
            assertThat(response.isSuccessful()).describedAs(response.message()).isTrue();
            var body = objectMapper.readValue(response.body().string(), Map.class);

            var data = (Map<String, Object>) body.get("data");
            var keys = (Map<String, Object>) data.get("keys");

            var key = (Map<String, Object>) keys.get("1");
            var publicKey = (String) key.get("public_key");
            return decodePublicKeyToJwk(publicKey, keyId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JWK decodePublicKeyToJwk(String publicKey, String keyId) {
        // PEM-encoded SPKI (Vault returns PEM for RSA and ECDSA keys)
        if (publicKey.contains("BEGIN PUBLIC KEY")) {
            try {
                return JWK.parseFromPEMEncodedObjects(publicKey);
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to parse PEM-encoded public key from Vault Transit", e);
            }
        }

        // if not PEM Encoded, it's an ED25519 key
        var rawBytes = Base64.getDecoder().decode(publicKey);
        try {
            return new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(rawBytes)).keyID(keyId).build();
        } catch (Exception e) {
            throw new EdcException("Unsupported key format from Vault Transit: %s".formatted(publicKey), e);
        }
    }

}
