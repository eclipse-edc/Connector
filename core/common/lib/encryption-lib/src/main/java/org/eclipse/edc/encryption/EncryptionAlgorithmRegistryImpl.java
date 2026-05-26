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
import java.util.function.Function;

public class EncryptionAlgorithmRegistryImpl implements EncryptionAlgorithmRegistry {

    private final Map<String, EncryptionAlgorithm> algorithms = new ConcurrentHashMap<>();

    public EncryptionAlgorithmRegistryImpl() {
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
        return apply(algorithm, plainText, a -> a.encrypt(plainText));
    }

    @Override
    public Result<String> decrypt(String algorithm, String cipherText) {
        return apply(algorithm, cipherText, a -> a.decrypt(cipherText));
    }

    private Result<String> apply(String algorithm, String input, Function<EncryptionAlgorithm, Result<String>> crypto) {
        if (algorithm == null) {
            return Result.success(input);
        }
        return Optional.ofNullable(algorithms.get(algorithm))
                .map(Result::success)
                .orElseGet(() -> Result.failure("Unsupported encryption algorithm: " + algorithm))
                .compose(crypto);
    }

}
