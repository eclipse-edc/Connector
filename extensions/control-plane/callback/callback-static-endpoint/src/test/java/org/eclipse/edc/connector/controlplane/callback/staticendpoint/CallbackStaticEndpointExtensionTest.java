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

package org.eclipse.edc.connector.controlplane.callback.staticendpoint;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class CallbackStaticEndpointExtensionTest {

    private final CallbackRegistry callbackRegistry = mock(CallbackRegistry.class);

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(CallbackRegistry.class, callbackRegistry);
    }

    @Test
    void initialize_shouldConfigureMultipleCallbacks(TestExtensionContext context, ObjectFactory factory) {
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
                "edc.callback.endpoint1.auth.codeid", callback.getAuthCodeId(),
                "edc.callback.endpoint1.auth.key", callback.getAuthKey());

        context.setConfig(ConfigFactory.fromMap(mapConfig));
        var extension = factory.constructInstance(CallbackStaticEndpointExtension.class);
        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(CallbackAddress.class);
        verify(callbackRegistry).register(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(callback);
    }

    @ParameterizedTest
    @ArgumentsSource(CallbackArgumentProvider.class)
    void initialize_shouldThrow_WhenWrongConfiguration(Map<String, String> callbackConfig, TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(callbackConfig));

        assertThatThrownBy(() -> {
            var extension = factory.constructInstance(CallbackStaticEndpointExtension.class);
            extension.initialize(context);
        }).isInstanceOf(EdcException.class);
    }

    static class CallbackArgumentProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    Map.of("edc.callback.cb.transactional", "false", "edc.cb.callback.events", "test"),
                    Map.of("edc.callback.cb.uri", "url", "edc.callback.cb.transactional", "false"),
                    Map.of("edc.callback.cb.uri", "url", "edc.callback.cb.transactional", "false", "edc.callback.cb.events", "test", "edc.callback.cb.auth.key", "test")
            ).map(Arguments::arguments);
        }
    }
}
