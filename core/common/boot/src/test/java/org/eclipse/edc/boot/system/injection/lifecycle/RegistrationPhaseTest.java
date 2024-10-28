/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection.lifecycle;

import org.eclipse.edc.boot.system.injection.ProviderMethod;
import org.eclipse.edc.boot.system.injection.ProviderMethodScanner;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RegistrationPhaseTest extends PhaseTest {

    private final ProviderMethodScanner scannerMock = mock();

    @Test
    void registerProviders_noProviderMethod() {
        var rp = new RegistrationPhase(new Phase(injector, container, context, monitor) {
        }, scannerMock);
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        rp.invokeProviderMethods();

        verify(scannerMock).nonDefaultProviders();
        verifyNoMoreInteractions(scannerMock);

    }

    @Test
    void registerProviders_withProvider_noDefault_notRegistered() {
        var scannerMock = mock(ProviderMethodScanner.class);
        var providerMethod = mock(ProviderMethod.class);
        when(providerMethod.isDefault()).thenReturn(false);
        when(providerMethod.invoke(any(), any())).thenReturn(new TestService());
        when(providerMethod.getReturnType()).thenAnswer(a -> TestService.class);
        when(context.hasService(TestService.class)).thenReturn(false);
        when(scannerMock.nonDefaultProviders()).thenReturn(Stream.of(providerMethod));

        var rp = new RegistrationPhase(new Phase(injector, container, context, monitor) {
        }, scannerMock);
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        rp.invokeProviderMethods();

        verify(context).registerService(eq(TestService.class), isA(TestService.class));
        verify(scannerMock).nonDefaultProviders();
        verifyNoMoreInteractions(scannerMock);

    }

    @Test
    void registerProviders_withProvider_isDefault_notRegistered() {
        var scannerMock = mock(ProviderMethodScanner.class);
        when(scannerMock.nonDefaultProviders()).thenReturn(Stream.empty());

        var rp = new RegistrationPhase(new Phase(injector, container, context, monitor) {
        }, scannerMock);
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        rp.invokeProviderMethods();

        verify(context, never()).registerService(any(), any());
        verify(scannerMock).nonDefaultProviders();
        verifyNoMoreInteractions(scannerMock);

    }

    @Test
    void registerProviders_withProvider_isDefault_isRegistered() {
        var scannerMock = mock(ProviderMethodScanner.class);

        var providerMethod = mock(ProviderMethod.class);
        when(providerMethod.isDefault()).thenReturn(true);
        when(providerMethod.invoke(any(), any())).thenReturn(new TestService());
        when(providerMethod.getReturnType()).thenAnswer(a -> TestService.class);

        when(context.hasService(TestService.class)).thenReturn(true);

        var rp = new RegistrationPhase(new Phase(injector, container, context, monitor) {
        }, scannerMock);
        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        rp.invokeProviderMethods();

        verify(context, never()).registerService(eq(TestService.class), isA(TestService.class));
    }


    private static class TestService {

    }
}
