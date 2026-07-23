/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.SignatureService;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProviderFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultSettings.forParticipant;

/**
 * Signature service using Hashicorp Vault with the Transit secrets engine.
 * <p>
 * Each vault partition is mapped to its own configuration / authentication settings, taken from a
 * {@link ParticipantContextConfig}, mirroring the behaviour of {@link HashicorpVault}. So theoretically, each
 * participant context's transit engine could live on a separate vault instance. If no configuration is found for a
 * given partition, the default (global) configuration is used, provided fallback is allowed.
 */
public class HashicorpVaultSignatureService implements SignatureService {

    private final Monitor monitor;
    private final ParticipantContextConfig participantContextConfig;
    private final HashicorpVaultConfig defaultConfig;
    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HashicorpVaultTokenProviderFactory tokenProviderFactory;

    public HashicorpVaultSignatureService(Monitor monitor, ParticipantContextConfig participantContextConfig,
                                          HashicorpVaultConfig defaultConfig, EdcHttpClient httpClient,
                                          ObjectMapper objectMapper, HashicorpVaultTokenProviderFactory tokenProviderFactory) {
        this.monitor = monitor;
        this.participantContextConfig = participantContextConfig;
        this.defaultConfig = defaultConfig;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.tokenProviderFactory = tokenProviderFactory;
    }

    @Override
    public Result<byte[]> sign(String key, byte[] payload, String signatureAlgorithm) {
        return sign(null, key, payload, signatureAlgorithm);
    }

    @Override
    public Result<Void> verify(String key, byte[] signingInput, byte[] signature, String signatureAlgorithm) {
        return verify(null, key, signingInput, signature, signatureAlgorithm);
    }

    @Override
    public Result<Void> rotate(String key, Map<String, Object> keyProperties) {
        return rotate(null, key, keyProperties);
    }

    /**
     * Signs the given payload with the key identified by the key parameter, within the given vault partition. Instead of
     * transmitting the key out of the Vault and signing the payload locally, the payload is transmitted to the remote
     * service, signed there with the specified key, and the signature is then transmitted back.
     *
     * @param vaultPartition     The vault partition to use, for example, a participant context ID. This might be null, which
     *                           indicates that the default partition should be used.
     * @param key                The key that is used for signing. This key must be available and accessible by this {@link SignatureService}
     * @param payload            A non-empty, non-null byte array to be signed. Note that this implementation will base64-encode the payload.
     * @param signatureAlgorithm Ignored for this implementation.
     * @return A Result containing the signature in the form {@code "vault:<key-version>:<base64-string>"} in bytes, or an error.
     */
    @Override
    public Result<byte[]> sign(@Nullable String vaultPartition, String key, byte[] payload, String signatureAlgorithm) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        var resolved = resolve(vaultPartition);
        var url = resolved.config().getVaultUrl() + resolved.config().getSecretsEnginePath() + "/sign/" + key;

        // omit key version from request body -> we'll always sign with the latest one
        var body = Map.of("input", Base64.getEncoder().encodeToString(payload));

        var request = new Request.Builder()
                .url(url)
                .header(VaultConstants.VAULT_TOKEN_HEADER, resolved.tokenProvider().vaultToken())
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
     * Verifies the given input data with the given signature within the given vault partition. Instead of transmitting the key
     * out of the Vault and performing the verification locally, the input and signature are transmitted to the remote service
     * and verified with the specified key, and the result is transmitted back.
     *
     * @param vaultPartition     The vault partition to use, for example, a participant context ID. This might be null, which
     *                           indicates that the default partition should be used.
     * @param key                The key that is used for signing. This key must exist in the Vault.
     * @param signingInput       The content from which the signature was created. Note that this implementation will base64-encode the payload
     * @param signature          The signature in the form {@code "vault:<key-version>:<base64-string>"}
     * @param signatureAlgorithm Ignored for this implementation
     * @return A Result indicating the success of the operation. {@link Result#success()} if the signature is valid.
     */
    @Override
    public Result<Void> verify(@Nullable String vaultPartition, String key, byte[] signingInput, byte[] signature, String signatureAlgorithm) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(signingInput, "signingInput cannot be null");
        Objects.requireNonNull(signature, "signature cannot be null");

        var resolved = resolve(vaultPartition);
        var url = resolved.config().getVaultUrl() + resolved.config().getSecretsEnginePath() + "/verify/" + key;

        // omit key version from request body -> we'll always sign with the latest one
        var body = Map.of("input", Base64.getEncoder().encodeToString(signingInput),
                "signature", new String(signature));

        var request = new Request.Builder()
                .url(url)
                .header(VaultConstants.VAULT_TOKEN_HEADER, resolved.tokenProvider().vaultToken())
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
     * Rotates the key in Hashicorp Transit engine within the given vault partition.
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID. This might be null, which
     *                       indicates that the default partition should be used.
     * @param key            The unique identifier for the key that should be rotated
     * @param ignored        Hashicorp's Transit secrets engine does not take any additional parameters
     * @return A result indicating the success of the operation
     */
    @Override
    public Result<Void> rotate(@Nullable String vaultPartition, String key, Map<String, Object> ignored) {
        Objects.requireNonNull(key, "key cannot be null");

        var resolved = resolve(vaultPartition);
        var url = resolved.config().getVaultUrl() + resolved.config().getSecretsEnginePath() + "/keys/" + key + "/rotate";

        var request = new Request.Builder()
                .url(url)
                .header(VaultConstants.VAULT_TOKEN_HEADER, resolved.tokenProvider().vaultToken())
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

    /**
     * Resolves the effective configuration / authentication token for the given vault partition.
     * If no config is found for the given partition, the default is returned, provided fallback is allowed.
     */
    private @NotNull ResolvedConfig resolve(@Nullable String vaultPartition) {
        if (vaultPartition == null) {
            return resolveDefault();
        }

        var settings = forParticipant(vaultPartition, participantContextConfig);
        if (settings != null) {
            return new ResolvedConfig(settings.config(), tokenProviderFactory.create(vaultPartition));
        }

        if (defaultConfig.isAllowFallback()) {
            return resolveDefault();
        }
        throw new IllegalArgumentException("No vault config found for partition '%s' and falling back to the default vault is not allowed".formatted(vaultPartition));
    }

    private ResolvedConfig resolveDefault() {
        return new ResolvedConfig(defaultConfig, tokenProviderFactory.create(null));
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

    private record ResolvedConfig(HashicorpVaultConfig config, HashicorpVaultTokenProvider tokenProvider) {
    }

}
