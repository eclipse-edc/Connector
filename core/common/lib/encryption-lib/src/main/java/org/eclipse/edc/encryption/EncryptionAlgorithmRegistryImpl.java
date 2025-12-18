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

package org.eclipse.edc.encryption;

import org.eclipse.edc.spi.result.Result;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptionAlgorithmRegistryImpl implements EncryptionAlgorithmRegistry {

    private final Map<String, EncryptionAlgorithm> algorithms = new ConcurrentHashMap<>();

    private final boolean failOnUnsupported;

    public EncryptionAlgorithmRegistryImpl(boolean failOnUnsupported) {
        this.failOnUnsupported = failOnUnsupported;
    }

    @Override
    public void register(String algorithm, EncryptionAlgorithm service) {
        algorithms.put(algorithm, service);
    }

    @Override
    public boolean supports(String algorithm) {
        return algorithms.containsKey(algorithm);
    }

    @Override
    public Result<String> encrypt(String algorithm, String plainText) {
        return Optional.ofNullable(algorithms.get(algorithm))
                .map(encryptionAlgorithm -> encryptionAlgorithm.encrypt(plainText))
                .orElseGet(() -> unsupportedAlgorithm(plainText, algorithm));
    }

    @Override
    public Result<String> decrypt(String algorithm, String cipherText) {
        return Optional.ofNullable(algorithms.get(algorithm))
                .map(encryptionAlgorithm -> encryptionAlgorithm.decrypt(cipherText))
                .orElseGet(() -> unsupportedAlgorithm(cipherText, algorithm));
    }

    private Result<String> unsupportedAlgorithm(String text, String algorithm) {
        if (failOnUnsupported) {
            return Result.failure("Unsupported encryption algorithm: " + algorithm);
        } else {
            return Result.success(text);
        }
    }
}
