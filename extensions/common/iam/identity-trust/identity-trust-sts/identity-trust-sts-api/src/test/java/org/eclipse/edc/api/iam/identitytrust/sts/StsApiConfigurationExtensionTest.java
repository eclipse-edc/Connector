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

package org.eclipse.edc.api.iam.identitytrust.sts;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.iam.identitytrust.sts.StsApiConfigurationExtension.DEFAULT_STS_PATH;
import static org.eclipse.edc.api.iam.identitytrust.sts.StsApiConfigurationExtension.DEFAULT_STS_PORT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class StsApiConfigurationExtensionTest {

    private final PortMappings portMappings = mock();
    private final Monitor monitor = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(PortMappings.class, portMappings);
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void initialize_shouldConfigureAndRegisterResource(StsApiConfigurationExtension extension) {
        var context = contextWithConfig(ConfigFactory.empty());

        extension.initialize(context);

        verify(portMappings).register(new PortMapping(ApiContext.STS, DEFAULT_STS_PORT, DEFAULT_STS_PATH));
    }

    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, config);
        context.initialize();
        return context;
    }
}
