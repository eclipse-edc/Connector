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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptionAlgorithmRegistryImplTest {

    private final EncryptionAlgorithm algorithm = mock();
    private final EncryptionAlgorithmRegistryImpl registry = new EncryptionAlgorithmRegistryImpl();

    @Test
    void registerAndSupports() {
        assertThat(registry.supports("echo")).isFalse();

        registry.register("echo", algorithm);
        assertThat(registry.supports("echo")).isTrue();
    }

    @Test
    void encryptAndDecryptWithRegisteredAlgorithm() {
        when(algorithm.encrypt(anyString())).then(a -> Result.success("enc:" + a.getArgument(0)));
        when(algorithm.decrypt(anyString())).then(a -> Result.success("dec:" + a.getArgument(0)));
        registry.register("echo", algorithm);

        var encryptResult = registry.encrypt("echo", "hello");
        assertTrue(encryptResult.succeeded());
        assertEquals("enc:hello", encryptResult.getContent());

        var decryptResult = registry.decrypt("echo", "ciph");
        assertTrue(decryptResult.succeeded());
        assertEquals("dec:ciph", decryptResult.getContent());
    }

    @Test
    void shouldReturnInput_whenAlgorithmIsNull() {
        assertThat(registry.encrypt(null, "any")).isSucceeded().isEqualTo("any");

        assertThat(registry.decrypt(null, "any")).isSucceeded().isEqualTo("any");
    }

}
