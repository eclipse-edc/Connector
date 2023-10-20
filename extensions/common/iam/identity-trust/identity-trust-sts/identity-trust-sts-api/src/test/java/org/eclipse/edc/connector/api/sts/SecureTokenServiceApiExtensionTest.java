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

package org.eclipse.edc.connector.api.sts;

import org.eclipse.edc.connector.api.sts.configuration.StsApiConfiguration;
import org.eclipse.edc.connector.api.sts.controller.SecureTokenServiceApiController;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.api.sts.StsApiConfigurationExtension.SETTINGS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class SecureTokenServiceApiExtensionTest {

    private final WebService webService = mock();

    private final WebServiceConfiguration configuration = WebServiceConfiguration.Builder.newInstance()
            .contextAlias(SETTINGS.getContextAlias())
            .path(SETTINGS.getDefaultPath())
            .port(SETTINGS.getDefaultPort())
            .build();
    
    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(StsApiConfiguration.class, new StsApiConfiguration(configuration));
    }

    @Test
    void initialize(ServiceExtensionContext context, SecureTokenServiceApiExtension extension) {

        extension.initialize(context);

        verify(webService).registerResource(eq(configuration.getContextAlias()), isA(SecureTokenServiceApiController.class));
    }

}
