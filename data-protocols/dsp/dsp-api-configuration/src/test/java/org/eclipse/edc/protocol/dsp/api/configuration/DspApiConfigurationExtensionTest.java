/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.api.configuration;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.spi.configuration.DspApiConfiguration;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfigurationExtension.CONTEXT_ALIAS;
import static org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfigurationExtension.DEFAULT_DSP_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfigurationExtension.DSP_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfigurationExtension.SETTINGS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspApiConfigurationExtensionTest {

    private final WebServiceConfigurer configurer = mock();
    private final WebServer webServer = mock();
    private final WebService webService = mock();
    private final TypeManager typeManager = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebServer.class, webServer);
        context.registerService(WebService.class, webService);
        context.registerService(WebServiceConfigurer.class, configurer);
        context.registerService(TypeManager.class, typeManager);
        TypeTransformerRegistry typeTransformerRegistry = mock();
        when(typeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);

        var webServiceConfiguration = WebServiceConfiguration.Builder.newInstance()
                .contextAlias(CONTEXT_ALIAS)
                .path("/path")
                .port(1234)
                .build();
        when(configurer.configure(any(), any(), any())).thenReturn(webServiceConfiguration);
        when(typeManager.getMapper(any())).thenReturn(mock());
    }

    @Test
    void initialize_noSettingsProvided_useDspDefault(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());
        when(context.getSetting(DSP_CALLBACK_ADDRESS, DEFAULT_DSP_CALLBACK_ADDRESS)).thenReturn(DEFAULT_DSP_CALLBACK_ADDRESS);

        extension.initialize(context);

        verify(configurer).configure(context, webServer, SETTINGS);
        var apiConfig = context.getService(DspApiConfiguration.class);
        assertThat(apiConfig.getContextAlias()).isEqualTo(CONTEXT_ALIAS);
        assertThat(apiConfig.getDspCallbackAddress()).isEqualTo(DEFAULT_DSP_CALLBACK_ADDRESS);
    }

    @Test
    void initialize_settingsProvided_useSettings(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        var webhookAddress = "http://webhook";
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "web.http.protocol.port", String.valueOf(1234),
                "web.http.protocol.path", "/path"))
        );
        when(context.getSetting(DSP_CALLBACK_ADDRESS, DEFAULT_DSP_CALLBACK_ADDRESS)).thenReturn(webhookAddress);

        extension.initialize(context);

        verify(configurer).configure(context, webServer, SETTINGS);
        var apiConfig = context.getService(DspApiConfiguration.class);
        assertThat(apiConfig.getContextAlias()).isEqualTo(CONTEXT_ALIAS);
        assertThat(apiConfig.getDspCallbackAddress()).isEqualTo(webhookAddress);
    }

    @Test
    void initialize_shouldRegisterWebServiceProviders(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(CONTEXT_ALIAS), isA(ObjectMapperProvider.class));
        verify(webService).registerResource(eq(CONTEXT_ALIAS), isA(JerseyJsonLdInterceptor.class));
    }

}
