/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection.lifecycle;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.boot.system.DependencyGraph;
import org.eclipse.edc.boot.system.TestObject;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.testextensions.DependentExtension;
import org.eclipse.edc.boot.system.testextensions.ProviderDefaultServicesExtension;
import org.eclipse.edc.boot.system.testextensions.ProviderExtension;
import org.eclipse.edc.boot.system.testextensions.RequiredDependentExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtensionLifecycleManagerTest {

    private final DefaultServiceExtensionContext context = new DefaultServiceExtensionContext(mock(), ConfigFactory.empty());

    @Test
    void shouldFollowLifecycleInOrder() {
        ServiceExtension serviceExtension = mock();
        var injectionContainer = new InjectionContainer<>(serviceExtension, Collections.emptySet(), Collections.emptyList());
        var orderVerifier = Mockito.inOrder(serviceExtension);

        ExtensionLifecycleManager.bootServiceExtensions(List.of(injectionContainer), context);

        orderVerifier.verify(serviceExtension).initialize(context);
        orderVerifier.verify(serviceExtension).prepare();
        orderVerifier.verify(serviceExtension).start();
    }

    @Test
    void shouldInvokeProvider() {
        var dependentExtension = new RequiredDependentExtension();
        var nonDefaultProvider = spy(new ProviderExtension());
        when(nonDefaultProvider.testObject()).thenCallRealMethod();

        boot(dependentExtension, nonDefaultProvider);

        verify(nonDefaultProvider, times(1)).testObject();
    }

    @Test
    void shouldInvokeProvider_whenDefaultIsProvided() {
        var dependentExtension = new RequiredDependentExtension();
        var defaultProvider = spy(new ProviderDefaultServicesExtension());
        var nonDefaultProvider = spy(new ProviderExtension());
        when(nonDefaultProvider.testObject()).thenCallRealMethod();

        boot(defaultProvider, dependentExtension, nonDefaultProvider);

        verify(defaultProvider, never()).testObject();
        verify(nonDefaultProvider, times(1)).testObject();
    }

    @Test
    void shouldInvokeDefaultProvider_whenNotProvided() {
        var dependentExtension = new RequiredDependentExtension();
        var defaultProvider = spy(new ProviderDefaultServicesExtension());
        when(defaultProvider.testObject()).thenCallRealMethod();

        boot(defaultProvider, dependentExtension);

        verify(defaultProvider, times(1)).testObject();
    }

    @Test
    void shouldInvokeDefaultProvider_whenDependencyIsOptional() {
        var dependentExtension = new DependentExtension();
        var defaultProvider = spy(new ProviderDefaultServicesExtension());
        when(defaultProvider.testObject()).thenCallRealMethod();

        boot(dependentExtension, defaultProvider);

        verify(defaultProvider, times(1)).testObject();
        assertThat(context.getService(TestObject.class)).isNotNull();
    }

    @Test
    void shouldInvokeNonDefaultProvider_whenDependencyIsOptional() {
        var dependentExtension = new DependentExtension();
        var defaultProvider = spy(new ProviderDefaultServicesExtension());
        when(defaultProvider.testObject()).thenCallRealMethod();
        var provider = spy(new ProviderExtension());
        when(provider.testObject()).thenCallRealMethod();

        boot(dependentExtension, defaultProvider, provider);

        verify(defaultProvider, never()).testObject();
        verify(provider, times(1)).testObject();
        assertThat(context.getService(TestObject.class)).isNotNull();
    }

    public void boot(ServiceExtension... serviceExtensions) {
        var injectionContainers = createInjectionContainers(mutableListOf(serviceExtensions));
        ExtensionLifecycleManager.bootServiceExtensions(injectionContainers, context);
    }

    public List<InjectionContainer<ServiceExtension>> createInjectionContainers(List<ServiceExtension> extensions) {
        return new DependencyGraph(context).of(extensions);
    }

}
