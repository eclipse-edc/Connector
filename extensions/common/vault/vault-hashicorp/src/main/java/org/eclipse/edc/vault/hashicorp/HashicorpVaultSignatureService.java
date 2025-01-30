/*
 *  Copyright (c) 2025 Confinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.SignatureService;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Signature service using Hashicorp Vault with the Transit secrets engine
 */
public class HashicorpVaultSignatureService implements SignatureService {

    private final Monitor monitor;
    private final HashicorpVaultSettings settings;
    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HashicorpVaultTokenProvider tokenProvider;


    public HashicorpVaultSignatureService(Monitor monitor, HashicorpVaultSettings settings, EdcHttpClient httpClient, ObjectMapper objectMapper, HashicorpVaultTokenProvider tokenProvider) {
        this.monitor = monitor;
        this.settings = settings;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Signs the given payload with the key identified by the key parameter. Instead of transmitting the key out of the Vault
     * and signing the payload locally, the payload is transmitted to the remote service, signed there with the specified key,
     * and the signature is then transmitted back.
     *
     * @param key                The key that is used for signing. This key must be available and accessible by this {@link SignatureService}
     * @param payload            A non-empty, non-null byte array to be signed. Note that this implementation will base64-encode the payload.
     * @param signatureAlgorithm Ignored for this implementation.
     * @return A Result containing the signature in the form {@code "vault:<key-version>:<base64-string>"} in bytes, or an error.
     * @throws UnsupportedOperationException if this operation is not supported by this {@link SignatureService}.
     * @throws IllegalArgumentException      if {@code signatureAlgorithm} is not recognized by this signing service
     */
    @Override
    public Result<byte[]> sign(String key, byte[] payload, String signatureAlgorithm) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        var url = settings.url() + settings.secretsEnginePath() + "/sign/" + key;

        // omit key version from request body -> we'll always sign with the latest one
        var body = Map.of("input", Base64.getEncoder().encodeToString(payload));

        var request = new Request.Builder()
                .url(url)
                .header(VaultConstants.VAULT_TOKEN_HEADER, tokenProvider.vaultToken())
                .post(jsonBody(body))
                .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    var r = objectMapper.readValue(response.body().string(), VaultConstants.MAP_TYPE_REFERENCE);

                    return ofNullable(r.get("data"))
                            .map(o -> (Map<?, ?>) o)
                            .map(dataObj -> dataObj.get("signature"))
                            .map(Object::toString)
                            .map(String::getBytes)
                            .map(Result::success)
                            .orElseGet(() -> Result.failure("JSON response did not contain signature"));
                }
                return Result.failure("Received empty body from Vault");
            }
            return Result.failure("Failed to sign payload with status %d, %s".formatted(response.code(), response.message()));
        } catch (IOException e) {
            monitor.warning("Error signing content: %s".formatted(e.getMessage()));
            return Result.failure("Error signing content: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Verifies the given input data with the given signature. Instead of transmitting the key out of the Vault
     * and performing the verification locally, the input and signature are transmitted to the remote service and verified
     * with the specified key, and the result is transmitted back.
     *
     * @param key                The key that is used for signing. This key must exist in the Vault.
     * @param signingInput       The content from which the signature was created. Note that this implementation will base64-encode the payload
     * @param signature          The signature in the form {@code "vault:<key-version>:<base64-string>"}
     * @param signatureAlgorithm Ignored for this implementation
     * @return A Result indicating the success of the operation. {@link Result#success()} if the signature is valid.
     */
    @Override
    public Result<Void> verify(String key, byte[] signingInput, byte[] signature, String signatureAlgorithm) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(signingInput, "signingInput cannot be null");
        Objects.requireNonNull(signature, "signature cannot be null");

        var url = settings.url() + settings.secretsEnginePath() + "/verify/" + key;

        // omit key version from request body -> we'll always sign with the latest one
        var body = Map.of("input", Base64.getEncoder().encodeToString(signingInput),
                "signature", new String(signature));

        var request = new Request.Builder()
                .url(url)
                .header(VaultConstants.VAULT_TOKEN_HEADER, tokenProvider.vaultToken())
                .post(jsonBody(body))
                .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    var r = objectMapper.readValue(response.body().string(), VaultConstants.MAP_TYPE_REFERENCE);

                    return ofNullable(r.get("data"))
                            .map(o -> (Map<?, ?>) o)
                            .map(dataObj -> dataObj.get("valid"))
                            .map(o -> Boolean.parseBoolean(o.toString()))
                            .map(b -> b ? Result.success() : Result.<Void>failure("Signature validation failed"))
                            .orElseGet(() -> Result.failure("JSON response did not contain valid verification data"));
                }
                return Result.failure("Received empty body from Vault");
            }
            return Result.failure("Failed to verify signature with status %d, %s".formatted(response.code(), response.message()));
        } catch (IOException e) {
            monitor.warning("Error signing content: %s".formatted(e.getMessage()));
            return Result.failure("Error signing content: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Rotates the key in Hashicorp Transit engine.
     *
     * @param key     The unique identifier for the key that should be rotated
     * @param ignored Hashicorp's Transit secrets engine does not take any additional parameters
     * @return A result indicating the success of the operation
     */
    @Override
    public Result<Void> rotate(String key, Map<String, Object> ignored) {

        Objects.requireNonNull(key, "key cannot be null");

        var url = settings.url() + settings.secretsEnginePath() + "/keys/" + key + "/rotate";

        var request = new Request.Builder()
                .url(url)
                .header(VaultConstants.VAULT_TOKEN_HEADER, tokenProvider.vaultToken())
                .post(RequestBody.create(new byte[0], null))
                .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    return Result.success();
                }
            }
            return Result.failure("Failed to rotate key, status %d, %s".formatted(response.code(), response.message()));
        } catch (IOException e) {
            var msg = "Error rotating key: %s".formatted(e.getMessage());
            monitor.warning(msg);
            return Result.failure(msg);
        }
    }

    private RequestBody jsonBody(Object body) {
        String jsonRepresentation;
        try {
            jsonRepresentation = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        return RequestBody.create(jsonRepresentation, VaultConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

}
