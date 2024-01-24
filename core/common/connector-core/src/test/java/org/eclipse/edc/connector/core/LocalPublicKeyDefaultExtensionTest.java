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

import com.nimbusds.jose.JOSEException;
import org.eclipse.edc.connector.core.security.LocalPublicKeyServiceImpl;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.PublicKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.core.LocalPublicKeyDefaultExtension.EDC_PUBLIC_KEYS_PREFIX;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class LocalPublicKeyDefaultExtensionTest {

    private final KeyParserRegistry keyParserRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(KeyParserRegistry.class, keyParserRegistry);
    }

    @Test
    void localPublicKeyService(LocalPublicKeyDefaultExtension extension, ServiceExtensionContext context) {
        assertThat(extension.localPublicKeyService()).isInstanceOf(LocalPublicKeyServiceImpl.class);
    }

    @Test
    void localPublicKeyService_withConfig(LocalPublicKeyDefaultExtension extension, ServiceExtensionContext context) throws JOSEException {

        var keys = Map.of(
                "key1.id", "key1",
                "key1.value", "value");

        when(keyParserRegistry.parse("value")).thenReturn(Result.success(mock(PublicKey.class)));
        when(context.getConfig(EDC_PUBLIC_KEYS_PREFIX)).thenReturn(ConfigFactory.fromMap(keys));
        var localPublicKeyService = extension.localPublicKeyService();
        extension.initialize(context);
        extension.prepare();

        assertThat(localPublicKeyService.resolveKey("key1")).isSucceeded();
    }

}