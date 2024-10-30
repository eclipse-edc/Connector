/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.boot.system;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.eclipse.edc.boot.monitor.MultiplexingMonitor;
import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.util.CyclicDependencyException;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtensionLoaderTest {

    private final ServiceLocator serviceLocator = mock();
    private final ServiceExtension coreExtension = new TestCoreExtension();
    private final ServiceExtensionContext context = mock();
    private final ExtensionLoader loader = new ExtensionLoader(serviceLocator);

    @BeforeAll
    public static void setup() {
        // mock default open telemetry
        GlobalOpenTelemetry.set(mock(OpenTelemetry.class));
    }

    @Test
    void loadMonitor_whenSingleMonitorExtension() {
        var mockedMonitor = mock(Monitor.class);
        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mockedMonitor);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertEquals(mockedMonitor, monitor);
    }

    @Test
    void loadMonitor_whenMultipleMonitorExtensions() {
        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mock(Monitor.class));
        exts.add(ConsoleMonitor::new);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertTrue(monitor instanceof MultiplexingMonitor);
    }

    @Test
    void loadMonitor_whenNoMonitorExtension() {
        var monitor = ExtensionLoader.loadMonitor(new ArrayList<>());

        assertTrue(monitor instanceof ConsoleMonitor);
    }

    @ParameterizedTest
    @ArgumentsSource(LogLevelVariantArgsProvider.class)
    void loadMonitor_programArgsSetConsoleMonitorLogLevel(String programArgs, ConsoleMonitor.Level level) {

        var monitor = ExtensionLoader.loadMonitor(new ArrayList<>(), programArgs);

        assertThat(monitor).extracting("level").isEqualTo(level);
    }

    @ParameterizedTest
    @ArgumentsSource(LogLevelWrongArgProvider.class)
    void loadMonitor_consoleMonitorDefaultLogLevelWhenWrongArgs(String programArgs, String expectedMessage) {
        assertThatThrownBy(() -> ExtensionLoader.loadMonitor(new ArrayList<>(), programArgs))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining(expectedMessage);

    }

    @Test
    void selectOpenTelemetryImpl_whenNoOpenTelemetry() {
        var openTelemetry = ExtensionLoader.selectOpenTelemetryImpl(emptyList());

        assertThat(openTelemetry).isEqualTo(GlobalOpenTelemetry.get());
    }

    @Test
    void selectOpenTelemetryImpl_whenSingleOpenTelemetry() {
        var customOpenTelemetry = mock(OpenTelemetry.class);

        var openTelemetry = ExtensionLoader.selectOpenTelemetryImpl(List.of(customOpenTelemetry));

        assertThat(openTelemetry).isSameAs(customOpenTelemetry);
    }

    @Test
    void selectOpenTelemetryImpl_whenSeveralOpenTelemetry() {
        var customOpenTelemetry1 = mock(OpenTelemetry.class);
        var customOpenTelemetry2 = mock(OpenTelemetry.class);

        Exception thrown = assertThrows(IllegalStateException.class,
                () -> ExtensionLoader.selectOpenTelemetryImpl(List.of(customOpenTelemetry1, customOpenTelemetry2)));
        assertEquals(thrown.getMessage(), "Found 2 OpenTelemetry implementations. Please provide only one OpenTelemetry service provider.");
    }

    @Test
    @DisplayName("No dependencies between service extensions")
    void loadServiceExtensions_noDependencies() {

        var service1 = new ServiceExtension() {
        };
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, coreExtension));

        var list = loader.loadServiceExtensions(context);

        assertThat(list).hasSize(2);
        assertThat(list).extracting(InjectionContainer::getInjectionTarget).contains(service1);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Locating two service extensions for the same service class ")
    void loadServiceExtensions_whenMultipleServices() {
        var service1 = new ServiceExtension() {
        };
        var service2 = new ServiceExtension() {
        };

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, service2, coreExtension));

        var list = loader.loadServiceExtensions(context);
        assertThat(list).hasSize(3);
        assertThat(list).extracting(InjectionContainer::getInjectionTarget).containsExactlyInAnyOrder(service1, service2, coreExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A DEFAULT service extension depends on a PRIMORDIAL one")
    void loadServiceExtensions_withBackwardsDependency() {
        var depending = new DependingExtension();
        var someExtension = new SomeExtension();
        var providing = new ProvidingExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(providing, depending, someExtension, coreExtension));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providing, depending, someExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }


    @Test
    @DisplayName("A service extension has a dependency on another one of the same loading stage")
    void loadServiceExtensions_withEqualDependency() {
        var depending = new DependingExtension() {
        };
        var coreService = new SomeExtension() {
        };

        var thirdService = new ServiceExtension() {
        };

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, thirdService, coreService, coreExtension));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactlyInAnyOrder(coreService, depending, thirdService, coreExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Two service extensions have a circular dependency")
    void loadServiceExtensions_withCircularDependency() {
        var s1 = new TestProvidingExtension2();
        var s2 = new TestProvidingExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A service extension has an unsatisfied dependency")
    void loadServiceExtensions_dependencyNotSatisfied() {
        var depending = new DependingExtension();
        var someExtension = new SomeExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, someExtension, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(EdcException.class).hasMessageContaining("The following injected fields were not provided:\nField \"someService\" of type ");
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Services extensions are sorted by dependency order")
    void loadServiceExtensions_dependenciesAreSorted() {
        var depending = new DependingExtension();
        var providingExtension = new ProvidingExtension();


        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, providingExtension, coreExtension));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, depending);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Should throw exception when no core dependency found")
    void loadServiceExtensions_noCoreDependencyShouldThrowException() {
        var depending = new DependingExtension();
        var coreService = new SomeExtension();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(EdcException.class);
    }

    @Test
    @DisplayName("Requires annotation influences ordering")
    void loadServiceExtensions_withAnnotation() {
        var depending = new DependingExtension();
        var providingExtension = new ProvidingExtension();
        var annotatedExtension = new AnnotatedExtension();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, annotatedExtension, providingExtension, coreExtension));

        var services = loader.loadServiceExtensions(context);

        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, depending, annotatedExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Requires annotation not satisfied")
    void loadServiceExtensions_withAnnotation_notSatisfied() {
        var annotatedExtension = new AnnotatedExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(annotatedExtension, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isNotInstanceOf(EdcInjectionException.class).isInstanceOf(EdcException.class);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Mixed requirement features work")
    void loadServiceExtensions_withMixedInjectAndAnnotation() {
        var providingExtension = new ProvidingExtension(); // provides SomeObject
        var anotherProvidingExt = new AnotherProvidingExtension(); //provides AnotherObject
        var mixedAnnotation = new MixedAnnotation();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(mixedAnnotation, providingExtension, coreExtension, anotherProvidingExt));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, anotherProvidingExt, mixedAnnotation);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Mixed requirement features introducing circular dependency")
    void loadServiceExtensions_withMixedInjectAndAnnotation_withCircDependency() {
        var s1 = new TestProvidingExtension3();
        var s2 = new TestProvidingExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @SafeVarargs
    private <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    private static class DependingExtension implements ServiceExtension {
        @Inject
        private SomeObject someService;
    }

    private static class SomeExtension implements ServiceExtension {
    }

    @Provides({ SomeObject.class })
    private static class ProvidingExtension implements ServiceExtension {
    }

    @Provides(AnotherObject.class)
    private static class AnotherProvidingExtension implements ServiceExtension {
    }

    @Provides({ SomeObject.class })
    private static class TestProvidingExtension implements ServiceExtension {
        @Inject
        AnotherObject obj;
    }

    @Provides({ AnotherObject.class })
    private static class TestProvidingExtension2 implements ServiceExtension {
        @Inject
        SomeObject obj;
    }

    @Provides({ AnotherObject.class })
    @Requires({ SomeObject.class })
    private static class TestProvidingExtension3 implements ServiceExtension {
    }

    @Requires(SomeObject.class)
    private static class AnnotatedExtension implements ServiceExtension {
    }

    @Requires(SomeObject.class)
    private static class MixedAnnotation implements ServiceExtension {
        @Inject
        private AnotherObject obj;
    }

    private static class SomeObject {
    }

    private static class AnotherObject {
    }


    private static class LogLevelWrongArgProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=INF", "Invalid value \"INF\" for the --log-level argument."),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=", "Value missing for the --log-level argument."),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "= INFO", "Invalid value \" INFO\" for the --log-level argument."),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + " INFO", "Value missing for the --log-level argument.")


            );
        }
    }

    private static class LogLevelVariantArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of("", ConsoleMonitor.Level.DEBUG), //default case
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=INFO", ConsoleMonitor.Level.INFO),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=DEBUG", ConsoleMonitor.Level.DEBUG),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=SEVERE", ConsoleMonitor.Level.SEVERE),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=WARNING", ConsoleMonitor.Level.WARNING),
                    Arguments.of(ConsoleMonitor.LEVEL_PROG_ARG + "=warning", ConsoleMonitor.Level.WARNING)


            );
        }
    }

    @BaseExtension
    private static class TestCoreExtension implements ServiceExtension {

    }
}
