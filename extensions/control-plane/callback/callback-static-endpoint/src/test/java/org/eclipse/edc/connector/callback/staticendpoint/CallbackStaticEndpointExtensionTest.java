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
import org.eclipse.edc.spi.system.configuration.Config;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.callback.staticendpoint.CallbackStaticEndpointExtension.EDC_CALLBACK_SETTING_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class CallbackStaticEndpointExtensionTest {

    CallbackStaticEndpointExtension extension;

    CallbackRegistry callbackRegistry = mock(CallbackRegistry.class);

    ServiceExtensionContext context;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(CallbackRegistry.class, callbackRegistry);
        extension = factory.constructInstance(CallbackStaticEndpointExtension.class);
        this.context = spy(context);
    }

    @Test
    void initialize_shouldConfigureMultipleCallbacks() {

        var cb1 = CallbackAddress.Builder.newInstance()
                .uri("http://url1")
                .transactional(false)
                .events(Set.of("transfer", "contract"))
                .build();
        var cb2 = CallbackAddress.Builder.newInstance()
                .uri("http://url2")
                .transactional(false)
                .events(Set.of("asset", "policy"))
                .authCodeId("codeId")
                .authKey("key")
                .build();

        var cb3 = CallbackAddress.Builder.newInstance()
                .uri("http://url3")
                .transactional(true)
                .events(Set.of("transfer"))
                .build();

        var callbacks = List.of(cb1, cb2, cb3);

        var cfg = IntStream.range(0, callbacks.size())
                .mapToObj(idx -> configureCallback(format("endpoint%s", idx), callbacks.get(idx)))
                .map(ConfigFactory::fromMap)
                .reduce(Config::merge)
                .orElseGet(ConfigFactory::empty);

        when(context.getConfig(EDC_CALLBACK_SETTING_PREFIX)).thenReturn(cfg.getConfig(EDC_CALLBACK_SETTING_PREFIX));

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(CallbackAddress.class);

        verify(callbackRegistry, times(callbacks.size())).register(captor.capture());

        assertThat(captor.getAllValues()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("events")
                .containsExactlyInAnyOrder(cb1, cb2, cb3);

    }

    @ParameterizedTest
    @ArgumentsSource(CallbackArgumentProvider.class)
    void initialize_shouldThrow_WhenWrongConfiguration(Map<String, String> callbackConfig) {

        var cfg = ConfigFactory.fromMap(callbackConfig);

        when(context.getConfig(EDC_CALLBACK_SETTING_PREFIX)).thenReturn(cfg.getConfig(EDC_CALLBACK_SETTING_PREFIX));

        assertThatThrownBy(() -> extension.initialize(context))
                .isInstanceOf(EdcException.class);

    }

    private Map<String, String> configureCallback(String name, CallbackAddress callbackAddress) {

        var config = new HashMap<String, String>();

        config.put(format("edc.callback.%s.uri", name), callbackAddress.getUri());
        config.put(format("edc.callback.%s.transactional", name), String.valueOf(callbackAddress.isTransactional()));

        if (callbackAddress.getEvents().size() > 0) {
            config.put(format("edc.callback.%s.events", name), String.join(", ", callbackAddress.getEvents()));
        }

        if (callbackAddress.getAuthCodeId() != null) {
            config.put(format("edc.callback.%s.auth-code-id", name), "codeId");
        }
        if (callbackAddress.getAuthKey() != null) {
            config.put(format("edc.callback.%s.auth-key", name), "key");
        }
        return config;
    }

    static class CallbackArgumentProvider implements ArgumentsProvider {
        CallbackArgumentProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Map.of("edc.callback.cb.url", "url", "edc.callback.events", "test"),
                    Map.of("edc.callback.cb.transactional", "false", "edc.callback.events", "test"),
                    Map.of("edc.callback.cb.url", "url", "edc.callback.transactional", "false")
            ).map(Arguments::arguments);
        }
    }
}
