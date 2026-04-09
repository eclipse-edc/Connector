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

package org.eclipse.edc.encryption.aes;

import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class AesEncryptionExtensionTest {

    @BeforeEach
    void setup(TestExtensionContext context) {
        context.registerService(EncryptionAlgorithmRegistry.class, mock());
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.encryption.aes.key.alias", "test-alias"
        )));
    }

    @Test
    void verifyEncryptionService(AesEncryptionExtension extension, TestExtensionContext ctx, EncryptionAlgorithmRegistry registry) {
        extension.initialize(ctx);
        verify(registry).register(eq("aes"), isA(AesEncryptionAlgorithm.class));
    }
}
