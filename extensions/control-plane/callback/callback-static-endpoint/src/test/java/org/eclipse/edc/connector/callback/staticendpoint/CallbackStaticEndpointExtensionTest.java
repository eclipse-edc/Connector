/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.callback.staticendpoint;

import org.eclipse.edc.connector.spi.callback.CallbackRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.callback.staticendpoint.CallbackStaticEndpointExtension.EDC_CALLBACK_SETTING_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class CallbackStaticEndpointExtensionTest {

    CallbackStaticEndpointExtension extension;
    CallbackRegistry callbackRegistry = mock(CallbackRegistry.class);

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(CallbackRegistry.class, callbackRegistry);
        extension = factory.constructInstance(CallbackStaticEndpointExtension.class);
    }

    @Test
    void initialize_shouldConfigureMultipleCallbacks(ServiceExtensionContext context) {
        var callback = CallbackAddress.Builder.newInstance()
                .uri("http://url2")
                .transactional(false)
                .events(Set.of("asset", "policy"))
                .authCodeId("codeId")
                .authKey("key")
                .build();

        var mapConfig = Map.of("edc.callback.endpoint1.uri", callback.getUri(),
                "edc.callback.endpoint1.transactional", String.valueOf(callback.isTransactional()),
                "edc.callback.endpoint1.events", String.join(" ,", callback.getEvents()),
                "edc.callback.endpoint1.auth-code-id", callback.getAuthCodeId(),
                "edc.callback.endpoint1.auth-key", callback.getAuthKey());

        var cfg = ConfigFactory.fromMap(mapConfig);

        when(context.getConfig(EDC_CALLBACK_SETTING_PREFIX)).thenReturn(cfg.getConfig(EDC_CALLBACK_SETTING_PREFIX));

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(CallbackAddress.class);

        verify(callbackRegistry).register(captor.capture());

        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(callback);

    }

    @ParameterizedTest
    @ArgumentsSource(CallbackArgumentProvider.class)
    void initialize_shouldThrow_WhenWrongConfiguration(Map<String, String> callbackConfig, ServiceExtensionContext context) {

        var cfg = ConfigFactory.fromMap(callbackConfig);

        when(context.getConfig(EDC_CALLBACK_SETTING_PREFIX)).thenReturn(cfg.getConfig(EDC_CALLBACK_SETTING_PREFIX));

        assertThatThrownBy(() -> extension.initialize(context))
                .isInstanceOf(EdcException.class);

    }

    static class CallbackArgumentProvider implements ArgumentsProvider {
        CallbackArgumentProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Map.of("edc.callback.cb.transactional", "false", "edc.cb.callback.events", "test"),
                    Map.of("edc.callback.cb.uri", "url", "edc.callback.cb.transactional", "false"),
                    Map.of("edc.callback.cb.uri", "url", "edc.callback.cb.transactional", "false", "edc.callback.cb.events", "test", "edc.callback.cb.auth-key", "test")
            ).map(Arguments::arguments);
        }
    }
}
