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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.boot.system.injection.lifecycle.ServiceProvider;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InjectionPointDefaultServiceSupplierTest {

    private final InjectionPointDefaultServiceSupplier supplier = new InjectionPointDefaultServiceSupplier();

    @Test
    void shouldRegisterDefaultService() {
        var injectionPoint = new ServiceInjectionPoint<>("any", mock());
        ServiceProvider serviceProvider = mock();
        when(serviceProvider.register(any())).thenReturn("service");
        injectionPoint.setDefaultServiceProvider(serviceProvider);
        ServiceExtensionContext context = mock();

        var service = supplier.provideFor(injectionPoint, context);

        assertThat(service).isEqualTo("service");
    }

    @Test
    void shouldReturnNull_whenNoDefaultServiceSet() {
        var injectionPoint = new ServiceInjectionPoint<>("any", mock(), false);
        ServiceExtensionContext context = mock();

        var service = supplier.provideFor(injectionPoint, context);

        assertThat(service).isNull();
    }

    @Test
    void shouldThrowException_whenNoDefaultServiceAndIsRequired() {
        var injectionPoint = new ServiceInjectionPoint<>("any", mock(), true);
        ServiceExtensionContext context = mock();

        assertThatThrownBy(() -> supplier.provideFor(injectionPoint, context)).isInstanceOf(EdcInjectionException.class);
    }
}
