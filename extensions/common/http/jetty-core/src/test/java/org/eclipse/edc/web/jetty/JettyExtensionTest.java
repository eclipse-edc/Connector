/*
 *  Copyright (c) 2025 Cofinity-X
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

package org.eclipse.edc.web.jetty;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class JettyExtensionTest {

    private final Monitor monitor = mock();

    @Test
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void shouldRegisterPortMapping(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var settings = Map.of("web.http.port", "11111", "web.http.path", "/path");
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));

        var extension = objectFactory.constructInstance(JettyExtension.class);

        extension.initialize(context);

        assertThat(extension).extracting("portMappingRegistry", type(PortMappingRegistry.class)).satisfies(portMappingRegistry -> {
            assertThat(portMappingRegistry.getAll()).containsOnly(new PortMapping("default", 11111, "/path"));
        });
        verify(context.getMonitor(), never()).warning(contains("web.http"));
    }

}
