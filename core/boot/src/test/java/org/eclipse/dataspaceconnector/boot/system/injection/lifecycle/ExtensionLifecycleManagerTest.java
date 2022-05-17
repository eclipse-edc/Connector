/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.boot.system.injection.lifecycle;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.injection.Injector;
import org.eclipse.dataspaceconnector.spi.system.injection.ProviderMethod;
import org.eclipse.dataspaceconnector.spi.system.injection.ProviderMethodScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ExtensionLifecycleManagerTest {

    private final Injector injector = mock(Injector.class);
    private final InjectionContainer<ServiceExtension> container = mock(InjectionContainer.class);
    private final ServiceExtensionContext context = mock(ServiceExtensionContext.class);
    private final ProviderMethodScanner providerMethodScanner = mock(ProviderMethodScanner.class);
    private final Monitor monitor = mock(Monitor.class);

    private ExtensionLifecycleManager manager;

    @BeforeEach
    void setup() {
        when(context.getMonitor()).thenReturn(monitor);
        manager = new ExtensionLifecycleManager(container, context, injector, providerMethodScanner);
    }

    @Test
    void initialize() {
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        when(container.validate(isA(ServiceExtensionContext.class))).thenReturn(Result.success());

        manager.initialize();

        verify(monitor).info(anyString());
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void initialize_validationFails() {
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        when(container.validate(isA(ServiceExtensionContext.class))).thenReturn(Result.failure("test-failure"));

        manager.initialize();

        verify(monitor).warning(startsWith("There were missing service registrations in extension"));
        verify(monitor).info(anyString());
        verifyNoMoreInteractions(monitor);
    }


    @Test
    void provide_noProviderMethod() {
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));

        manager.provide();

        verify(providerMethodScanner).nonDefaultProviders();
        verifyNoMoreInteractions(providerMethodScanner);
    }

    @Test
    void provide_withProvider_noDefault_notRegistered() {
        var providerMethod = mock(ProviderMethod.class);
        when(providerMethod.isDefault()).thenReturn(false);
        when(providerMethod.invoke(any(), any())).thenReturn(new TestService());
        when(providerMethod.getReturnType()).thenAnswer(a -> TestService.class);
        when(context.hasService(TestService.class)).thenReturn(false);
        when(providerMethodScanner.nonDefaultProviders()).thenReturn(Set.of(providerMethod));

        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));

        manager.provide();

        verify(context).registerService(eq(TestService.class), isA(TestService.class));
        verify(providerMethodScanner).nonDefaultProviders();
        verifyNoMoreInteractions(providerMethodScanner);
    }

    @Test
    void provide_withProvider_isDefault_notRegistered() {
        when(providerMethodScanner.nonDefaultProviders()).thenReturn(Set.of());
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));

        manager.provide();

        verify(context, never()).registerService(any(), any());
        verify(providerMethodScanner).nonDefaultProviders();
        verifyNoMoreInteractions(providerMethodScanner);

    }

    @Test
    void provide_withProvider_isDefault_isRegistered() {
        var providerMethod = mock(ProviderMethod.class);
        when(providerMethod.isDefault()).thenReturn(true);
        when(providerMethod.invoke(any(), any())).thenReturn(new TestService());
        when(providerMethod.getReturnType()).thenAnswer(a -> TestService.class);

        when(context.hasService(TestService.class)).thenReturn(true);
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));

        manager.provide();

        verify(context, never()).registerService(eq(TestService.class), isA(TestService.class));
    }

    @Test
    void start() {
        var extension = mock(ServiceExtension.class);
        when(container.getInjectionTarget()).thenReturn(extension);

        manager.start();

        verify(extension).start();
        verify(extension).name();
        verifyNoMoreInteractions(extension);
    }

    private static class TestService {

    }
}