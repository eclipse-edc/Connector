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

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.http.api.configuration.DspApiBaseConfigurationExtension.DEFAULT_PROTOCOL_PATH;
import static org.eclipse.edc.protocol.dsp.http.api.configuration.DspApiBaseConfigurationExtension.DEFAULT_PROTOCOL_PORT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspApiBaseConfigurationExtensionTest {
    
    private final PortMappingRegistry portMappingRegistry = mock();
    private final WebService webService = mock();
    
    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(Hostname.class, () -> "hostname");
        context.registerService(PortMappingRegistry.class, portMappingRegistry);
        context.registerService(WebService.class, webService);
    }
    
    @Test
    void shouldComposeProtocolWebhook_whenNotConfigured(DspApiBaseConfigurationExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());
        var expectedWebhook = "http://hostname:%s%s".formatted(DEFAULT_PROTOCOL_PORT, DEFAULT_PROTOCOL_PATH);
        
        extension.initialize(context);
        
        verify(portMappingRegistry).register(new PortMapping(ApiContext.PROTOCOL, DEFAULT_PROTOCOL_PORT, DEFAULT_PROTOCOL_PATH));
        assertThat(extension.dspBaseWebhookAddress().get()).isEqualTo(expectedWebhook);
    }
    
    @Test
    void shouldUseConfiguredProtocolWebhook(ServiceExtensionContext context, ObjectFactory factory) {
        var webhookAddress = "http://webhook";
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "web.http.protocol.port", String.valueOf(1234),
                "web.http.protocol.path", "/path",
                "edc.dsp.callback.address", webhookAddress))
        );
        var extension = factory.constructInstance(DspApiBaseConfigurationExtension.class);
        
        extension.initialize(context);
        
        verify(portMappingRegistry).register(new PortMapping(ApiContext.PROTOCOL, 1234, "/path"));
        assertThat(extension.dspBaseWebhookAddress().get()).isEqualTo(webhookAddress);
    }
    
    @Test
    void initialize_shouldRegisterWebServiceProviders(DspApiBaseConfigurationExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        
        verify(webService).registerResource(eq(ApiContext.PROTOCOL), isA(ObjectMapperProvider.class));
    }
}
