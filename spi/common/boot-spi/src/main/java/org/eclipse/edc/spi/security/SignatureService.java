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

package org.eclipse.edc.spi.security;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Allows signing and verifying of data.
 */
@ExtensionPoint
public interface SignatureService {
    /**
     * Signs the given payload with the key identified by the key parameter. Instead of transmitting the key out of the Vault
     * and signing the payload locally, the payload is transmitted to the remote service, signed there with the specified key,
     * and the signature is then transmitted back.
     *
     * @param key                The key that is used for signing. This key must be available and accessible by this {@link SignatureService}
     * @param payload            A non-empty, non-null byte array to be signed.
     * @param signatureAlgorithm A string identifying the signature algorithm
     * @return A Result containing the signature bytes, or an error.
     * @throws UnsupportedOperationException if this operation is not supported by this {@link SignatureService}.
     * @throws IllegalArgumentException      if {@code signatureAlgorithm} is not recognized by this signing service
     */
    Result<byte[]> sign(String key, byte[] payload, String signatureAlgorithm);

    /**
     * Verifies the given input data with the given signature. Instead of transmitting the key out of the Vault
     * and performing the verification locally, the input and signature are transmitted to the Vault and verified remotely
     * with the specified key, and the result is transmitted back.
     *
     * @param key                The key that is used for signing. This key must exist in the Vault.
     * @param signingInput       The content from which the signature was created.
     * @param signature          The signature
     * @param signatureAlgorithm A string identifying the signature algorithm
     * @return A Result containing the signature bytes, or an error.
     * @throws UnsupportedOperationException if this operation is not supported by this {@link SignatureService}.
     * @throws IllegalArgumentException      if {@code signatureAlgorithm} is not recognized by this signing service
     */
    Result<Void> verify(String key, byte[] signingInput, byte[] signature, String signatureAlgorithm);


    /**
     * Manually rotates the key. This means provisioning a new version of the key with new key material.
     * The behaviour of this method is quite loosely defined, as it is up to the implementation to define the following:
     * <ul>
     *     <li>whether the old key gets disabled automatically</li>
     *     <li>rotation period, i.e. the time until the old key gets disabled</li>
     *     <li>time-to-live, validity period for the new key. Can be globally defined or configured using the {@code keyProperties} </li>
     *     <li>key parameters, such as key algorithm, EC curve, bit length etc. Can be globally defined or configured using the {@code keyProperties}</li>
     * </ul>
     *
     * @param keyId         The unique identifier for the key that should be rotated
     * @param keyProperties A set of parameters that are supported by the implementation.
     * @return {@link Result#success()} if the rotation process was successfully started, {@link Result#failure(String)} otherwise.
     * @throws UnsupportedOperationException if this operation is not supported by this {@link SignatureService}.
     * @throws IllegalArgumentException      if {@code keyProperties} contains illegal parameters, or required parameters are missing.
     */
    Result<Void> rotate(String keyId, Map<String, Object> keyProperties);

}
