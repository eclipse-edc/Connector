/*
 *  Copyright (c) 2020 - 2024 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.boot.system.runtime;

import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.boot.system.testextensions.AlternativeFeatureExtension;
import org.eclipse.edc.boot.system.testextensions.DefaultFeatureExtension;
import org.eclipse.edc.boot.system.testextensions.RequiredDependentExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseRuntimeTest {

    private final Monitor monitor = mock();
    private final ServiceLocator serviceLocator = mock();
    private final BaseRuntime runtime = new BaseRuntimeFixture(monitor, serviceLocator);

    @Test
    void shouldBoot() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf());

        runtime.boot(true);

        verify(monitor, never()).severe(anyString(), any());
    }

    @Test
    void shouldNotBootWithException() {
        var extension = spy(extensionThatRegisters(Object.class, "any"));

        doThrow(new EdcException("Failed to start base extension")).when(extension).start();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(extension));

        assertThatThrownBy(() -> runtime.boot(true)).isInstanceOf(EdcException.class);
        verify(monitor).severe(startsWith("Error booting runtime: Failed to start base extension"), any(EdcException.class));
    }

    @Test
    void shouldMarkContextReady_onlyAfterAllExtensionsStarted() {
        var contextRef = new AtomicReference<ServiceExtensionContext>();
        var readyDuringStart = new AtomicBoolean(true);
        var extension = new ServiceExtension() {
            @Override
            public void initialize(ServiceExtensionContext context) {
                contextRef.set(context);
            }

            @Override
            public void start() {
                readyDuringStart.set(contextRef.get().isReady());
            }
        };
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(extension));

        runtime.boot(true);

        assertThat(readyDuringStart).isFalse();
        assertThat(contextRef.get().isReady()).isTrue();
    }

    @Test
    void shouldLoadConfiguration() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf());

        runtime.boot(true);

        verify(serviceLocator).loadImplementors(ConfigurationExtension.class, false);
    }

    @Test
    void shouldShutdownAllExtensionsAndCleanup_whenOneThrowsException() {
        ServiceExtension successful = mock();
        ServiceExtension unsuccessful = mock();
        doThrow(new RuntimeException("shutdown error")).when(unsuccessful).shutdown();
        doThrow(new RuntimeException("shutdown error")).when(unsuccessful).cleanup();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean()))
                .thenReturn(mutableListOf(successful, unsuccessful));

        runtime.boot(true);

        assertThatNoException().isThrownBy(runtime::shutdown);
        verify(successful).shutdown();
        verify(successful).cleanup();
        verify(unsuccessful).shutdown();
        verify(unsuccessful).cleanup();
    }

    @Test
    void shouldLogProblems_whenDependencyGraphFails() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean()))
                .thenReturn(mutableListOf(extensionThatRequiresConfig()));

        assertThatThrownBy(() -> runtime.boot(true)).isInstanceOf(EdcException.class);

        verify(monitor).severe(contains("is required by"));
        verify(monitor, never()).info(contains(" ready"));
    }

    @Test
    void shouldLoadOnlyExtensionsOfSelectedFeature() {
        var dependent = new RequiredDependentExtension();
        when(serviceLocator.loadImplementors(ConfigurationExtension.class, false))
                .thenReturn(List.of(configurationExtension(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative"))));
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean()))
                .thenReturn(mutableListOf(dependent, new DefaultFeatureExtension(), new AlternativeFeatureExtension()));

        runtime.boot(true);

        assertThat(dependent.getTestObject().getDescription()).isEqualTo("alternative");
        verify(monitor).debug(contains("skipped extensions: " + DefaultFeatureExtension.NAME));
    }

    @Test
    void shouldFailBoot_whenFeatureValueUnknown() {
        when(serviceLocator.loadImplementors(ConfigurationExtension.class, false))
                .thenReturn(List.of(configurationExtension(Map.of(DefaultFeatureExtension.FEATURE_KEY, "unknown"))));
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean()))
                .thenReturn(mutableListOf(new DefaultFeatureExtension(), new AlternativeFeatureExtension()));

        assertThatThrownBy(() -> runtime.boot(true))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining(DefaultFeatureExtension.FEATURE_KEY);
    }

    private ConfigurationExtension configurationExtension(Map<String, String> settings) {
        return new ConfigurationExtension() {
            @Override
            public Config getConfig() {
                return ConfigFactory.fromMap(settings);
            }
        };
    }

    @NotNull
    private <T> ServiceExtension extensionThatRegisters(Class<T> serviceClass, T service) {
        return new ServiceExtension() {
            @Override
            public void initialize(ServiceExtensionContext context) {
                context.registerService(serviceClass, service);
            }
        };
    }

    @NotNull
    private <T> ServiceExtension extensionThatRequiresConfig() {
        return new ServiceExtension() {

            @Setting(key = "test.org.edc.setting")
            private String setting;

        };
    }

    private static class BaseRuntimeFixture extends BaseRuntime {

        private final Monitor monitor;

        BaseRuntimeFixture(Monitor monitor, ServiceLocator serviceLocator) {
            super(serviceLocator);
            this.monitor = monitor;
        }

        @Override
        protected @NotNull Monitor createMonitor() {
            return monitor;
        }
    }
}
