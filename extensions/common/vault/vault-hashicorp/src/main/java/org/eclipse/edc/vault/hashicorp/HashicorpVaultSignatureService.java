/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.SignatureService;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultClient;

import java.util.Map;

/**
 * Signature service using Hashicorp Vault with the Transit secrets engine
 */
public class HashicorpVaultSignatureService implements SignatureService {

    private final HashicorpVaultClient vaultClient;
    private final Monitor monitor;

    public HashicorpVaultSignatureService(HashicorpVaultClient vaultClient, Monitor monitor) {
        this.vaultClient = vaultClient;
        this.monitor = monitor;
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
        return vaultClient.sign(key, payload)
                .onFailure(f -> monitor.warning("Error signing payload: %s".formatted(f.getFailureDetail())))
                .map(String::getBytes);
    }

    /**
     * Verifies the given input data with the given signature. Instead of transmitting the key out of the Vault
     * and performing the verification locally, the input and signature are transmitted to the Vault and verified remotely
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
        return vaultClient.verify(key, signingInput, new String(signature))
                .onFailure(f -> monitor.warning("Error verifying signature: %s".formatted(f.getFailureDetail())));
    }

    /**
     * Rotates the key in Hashicorp Transit engine.
     *
     * @param keyId   The unique identifier for the key that should be rotated
     * @param ignored Hashicorp's Transit secrets engine does not take any additional parameters
     * @return A result indicating the success of the operation
     */
    @Override
    public Result<Void> rotate(String keyId, Map<String, Object> ignored) {
        return vaultClient.rotate(keyId)
                .onFailure(f -> monitor.warning("Error rotating key: %s".formatted(f.getFailureDetail())))
                .map(version -> null);
    }
}
