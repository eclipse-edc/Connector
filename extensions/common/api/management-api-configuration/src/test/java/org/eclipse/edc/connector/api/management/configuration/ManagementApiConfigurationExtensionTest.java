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

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.DEFAULT_MANAGEMENT_PATH;
import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.DEFAULT_MANAGEMENT_PORT;
import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.MANAGEMENT_SCOPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ManagementApiConfigurationExtensionTest {

    private final PortMappingRegistry portMappingRegistry = mock();
    private final Monitor monitor = mock();
    private final WebService webService = mock();
    private final JsonLd jsonLd = mock();
    private ManagementApiConfigurationExtension extension;

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
        extension = factory.constructInstance(ManagementApiConfigurationExtension.class);
    }

    @Test
    void initialize_shouldConfigureAndRegisterResource() {
        var context = contextWithConfig(ConfigFactory.empty());

        extension.initialize(context);

        verify(portMappingRegistry).register(new PortMapping(ApiContext.MANAGEMENT, DEFAULT_MANAGEMENT_PORT, DEFAULT_MANAGEMENT_PATH));
        verify(webService).registerResource(eq(ApiContext.MANAGEMENT), isA(AuthenticationRequestFilter.class));
        verify(webService).registerResource(eq(ApiContext.MANAGEMENT), isA(JerseyJsonLdInterceptor.class));
        verify(webService).registerResource(eq(ApiContext.MANAGEMENT), isA(ObjectMapperProvider.class));

        verify(jsonLd).registerNamespace(VOCAB, EDC_NAMESPACE, MANAGEMENT_SCOPE);
        verify(jsonLd).registerNamespace(EDC_PREFIX, EDC_NAMESPACE, MANAGEMENT_SCOPE);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, MANAGEMENT_SCOPE);
    }

    @Test
    void initialize_withContextEnabled(ObjectFactory factory, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.management.context.enabled", "true")));

        factory.constructInstance(ManagementApiConfigurationExtension.class).initialize(context);

        verify(jsonLd, times(0)).registerNamespace(any(), any(), any());
        verify(jsonLd).registerContext(EDC_CONNECTOR_MANAGEMENT_CONTEXT, MANAGEMENT_SCOPE);
    }

    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, config);
        context.initialize();
        return context;
    }
}
