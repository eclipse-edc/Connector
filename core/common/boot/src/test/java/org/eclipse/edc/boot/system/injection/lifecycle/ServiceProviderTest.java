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

import org.eclipse.edc.boot.system.injection.ProviderMethod;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServiceProviderTest {

    @Test
    void shouldRegisterService() throws NoSuchMethodException {
        TestService service = mock();
        var extension = new TestServiceExtension(service);
        var method = extension.getClass().getDeclaredMethod("testService");
        method.setAccessible(true);
        var providerMethod = new ProviderMethod(method);

        ServiceExtensionContext context = mock();
        var serviceProvider = new ServiceProvider(providerMethod, extension);

        var registered = serviceProvider.apply(context);

        assertThat(registered).isEqualTo(service);
        verify(context).registerService(TestService.class, service);
    }

    private record TestServiceExtension(TestService service) implements ServiceExtension {

        @Provider
        public TestService testService() {
            return service;
        }

    }

    private interface TestService {
    }
}
