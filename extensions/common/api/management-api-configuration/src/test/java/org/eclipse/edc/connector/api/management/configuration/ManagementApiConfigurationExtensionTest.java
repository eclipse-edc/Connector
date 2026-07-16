/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.configuration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V5;
import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.DEFAULT_MANAGEMENT_PATH;
import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.DEFAULT_MANAGEMENT_PORT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ManagementApiConfigurationExtensionTest {

    private final PortMappingRegistry portMappingRegistry = mock();
    private final WebService webService = mock();
    private final JsonLd jsonLd = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        TypeTransformerRegistry typeTransformerRegistry = mock();
        TypeTransformerRegistry contextTypeTransformerRegistry = mock();
        when(typeTransformerRegistry.forContext(any())).thenReturn(contextTypeTransformerRegistry);
        when(contextTypeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(WebService.class, webService);
        context.registerService(PortMappingRegistry.class, portMappingRegistry);
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
        context.registerService(TypeManager.class, new JacksonTypeManager());
        context.registerService(JsonLd.class, jsonLd);
    }

    @Test
    void initialize_shouldConfigureAndRegisterResource(ObjectFactory factory, TestExtensionContext context) {
        context.setConfig(ConfigFactory.empty());

        var extension = factory.constructInstance(ManagementApiConfigurationExtension.class);
        extension.initialize(context);

        verify(portMappingRegistry).register(new PortMapping(ApiContext.MANAGEMENT, DEFAULT_MANAGEMENT_PORT, DEFAULT_MANAGEMENT_PATH));
        verify(webService).registerResource(eq(ApiContext.MANAGEMENT), isA(ObjectMapperProvider.class));

        verify(jsonLd).registerContext(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2, MANAGEMENT_SCOPE_V4);
        verify(jsonLd).registerContext(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2, MANAGEMENT_SCOPE_V5);
    }

}
