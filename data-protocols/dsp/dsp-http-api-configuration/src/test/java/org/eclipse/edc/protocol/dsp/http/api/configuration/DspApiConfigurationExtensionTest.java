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

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.http.api.configuration.DspApiConfigurationExtension.DSP_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.http.api.configuration.DspApiConfigurationExtension.SETTINGS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
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
    private final JsonLd jsonLd = mock();


    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebServer.class, webServer);
        context.registerService(WebService.class, webService);
        context.registerService(WebServiceConfigurer.class, configurer);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(Hostname.class, () -> "hostname");
        context.registerService(JsonLd.class, jsonLd);
        TypeTransformerRegistry typeTransformerRegistry = mock();
        when(typeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);

        var webServiceConfiguration = WebServiceConfiguration.Builder.newInstance()
                .path("/path")
                .port(1234)
                .build();
        when(configurer.configure(any(Config.class), any(), any())).thenReturn(webServiceConfiguration);
        when(typeManager.getMapper(any())).thenReturn(mock());
    }

    @Test
    void shouldComposeProtocolWebhook_whenNotConfigured(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(configurer).configure(any(Config.class), eq(webServer), eq(SETTINGS));
        assertThat(context.getService(ProtocolWebhook.class).url()).isEqualTo("http://hostname:1234/path");
    }

    @Test
    void shouldUseConfiguredProtocolWebhook(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        var webhookAddress = "http://webhook";
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "web.http.protocol.port", String.valueOf(1234),
                "web.http.protocol.path", "/path"))
        );
        when(context.getSetting(eq(DSP_CALLBACK_ADDRESS), any())).thenReturn(webhookAddress);

        extension.initialize(context);

        verify(configurer).configure(any(Config.class), eq(webServer), eq(SETTINGS));
        assertThat(context.getService(ProtocolWebhook.class).url()).isEqualTo("http://webhook");
    }

    @Test
    void initialize_shouldRegisterWebServiceProviders(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(ApiContext.PROTOCOL), isA(ObjectMapperProvider.class));
        verify(webService).registerResource(eq(ApiContext.PROTOCOL), isA(JerseyJsonLdInterceptor.class));
    }

    @Test
    void initialize_shouldRegisterNamespaces(DspApiConfigurationExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(jsonLd).registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, DSP_SCOPE);
        verify(jsonLd).registerNamespace(DCT_PREFIX, DCT_SCHEMA, DSP_SCOPE);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, DSP_SCOPE);
        verify(jsonLd).registerNamespace(VOCAB, EDC_NAMESPACE, DSP_SCOPE);
        verify(jsonLd).registerNamespace(EDC_PREFIX, EDC_NAMESPACE, DSP_SCOPE);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, DSP_SCOPE);
    }

}
