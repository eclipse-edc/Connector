/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.core;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.keys.LocalPublicKeyServiceImpl;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.core.LocalPublicKeyDefaultExtension.EDC_PUBLIC_KEYS_PREFIX;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class LocalPublicKeyDefaultExtensionTest {

    private final KeyParserRegistry keyParserRegistry = mock();

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(KeyParserRegistry.class, keyParserRegistry);
    }

    @Test
    void localPublicKeyService(LocalPublicKeyDefaultExtension extension) {
        assertThat(extension.localPublicKeyService()).isInstanceOf(LocalPublicKeyServiceImpl.class);
    }

    @Test
    void localPublicKeyService_withValueConfig(ObjectFactory factory, TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                EDC_PUBLIC_KEYS_PREFIX + ".key1.id", "key1",
                EDC_PUBLIC_KEYS_PREFIX + ".key1.value", "value"
        )));
        when(keyParserRegistry.parsePublic("value")).thenReturn(Result.success(mock(PublicKey.class)));

        var extension = factory.constructInstance(LocalPublicKeyDefaultExtension.class);
        var localPublicKeyService = extension.localPublicKeyService();
        extension.prepare();

        assertThat(localPublicKeyService.resolveKey("key1")).isSucceeded();
    }

    @Test
    void localPublicKeyService_withPathConfig(ObjectFactory factory, TestExtensionContext context) {
        var path = TestUtils.getResource("rsa_2048.pem");
        var value = TestUtils.getResourceFileContentAsString("rsa_2048.pem");
        context.setConfig(ConfigFactory.fromMap(Map.of(
                EDC_PUBLIC_KEYS_PREFIX + ".key1.id", "key1",
                EDC_PUBLIC_KEYS_PREFIX + ".key1.path", Paths.get(path).toString()
        )));

        when(keyParserRegistry.parsePublic(value)).thenReturn(Result.success(mock(PublicKey.class)));

        var extension = factory.constructInstance(LocalPublicKeyDefaultExtension.class);
        var localPublicKeyService = extension.localPublicKeyService();
        extension.prepare();

        assertThat(localPublicKeyService.resolveKey("key1")).isSucceeded();
    }

    @Test
    void localPublicKeyService_shouldRaiseException_withoutValueOrPath(ObjectFactory factory, TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                EDC_PUBLIC_KEYS_PREFIX + ".key1.id", "key1"
        )));

        var extension = factory.constructInstance(LocalPublicKeyDefaultExtension.class);

        assertThatThrownBy(extension::prepare).isInstanceOf(EdcException.class);
    }

}
