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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptionAlgorithmRegistryImplTest {

    private final EncryptionAlgorithm algorithm = mock();

    @Test
    void registerAndSupports() {
        var registry = new EncryptionAlgorithmRegistryImpl(true);
        assertFalse(registry.supports("echo"));

        registry.register("echo", algorithm);
        assertTrue(registry.supports("echo"));
    }

    @Test
    void encryptAndDecryptWithRegisteredAlgorithm() {
        var registry = new EncryptionAlgorithmRegistryImpl(true);
        when(algorithm.encrypt(anyString())).then(a -> Result.success("enc:" + a.getArgument(0)));
        when(algorithm.decrypt(anyString())).then(a -> Result.success("dec:" + a.getArgument(0)));
        registry.register("echo", algorithm);

        Result<String> encryptResult = registry.encrypt("echo", "hello");
        assertTrue(encryptResult.succeeded());
        assertEquals("enc:hello", encryptResult.getContent());

        Result<String> decryptResult = registry.decrypt("echo", "ciph");
        assertTrue(decryptResult.succeeded());
        assertEquals("dec:ciph", decryptResult.getContent());
    }

    @Test
    void unsupportedAlgorithm_whenFailOnUnsupported_true_returnsFailure() {
        var registry = new EncryptionAlgorithmRegistryImpl(true);

        Result<String> res = registry.encrypt("unknown", "plain");
        assertFalse(res.succeeded());
    }

    @Test
    void unsupportedAlgorithm_whenFailOnUnsupported_false_returnsPlainText() {
        var registry = new EncryptionAlgorithmRegistryImpl(false);

        Result<String> res = registry.encrypt("unknown", "plain");
        assertTrue(res.succeeded());
        assertEquals("plain", res.getContent());
    }
}
