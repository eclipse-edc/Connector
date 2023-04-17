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
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurerImpl;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspApiConfigurationExtensionTest {
    
    private final WebServiceConfigurer configurer = mock(WebServiceConfigurerImpl.class);
    private final WebServer webServer = mock(WebServer.class);

    private DspApiConfigurationExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(WebServer.class, webServer);
        context.registerService(WebServiceConfigurer.class, configurer);
        extension = factory.constructInstance(DspApiConfigurationExtension.class);
        
        var webServiceConfiguration = WebServiceConfiguration.Builder.newInstance()
                .contextAlias(CONTEXT_ALIAS)
                .path("/path")
                .port(1234)
                .build();
        when(configurer.configure(any(), any(), any())).thenReturn(webServiceConfiguration);
    }
    
    @Test
    void initialize_noSettingsProvided_useDspDefault(ServiceExtensionContext context) {
        var spyContext = spy(context);
        when(spyContext.getConfig()).thenReturn(ConfigFactory.empty());
        when(spyContext.getSetting(DSP_CALLBACK_ADDRESS, DEFAULT_DSP_CALLBACK_ADDRESS)).thenReturn(DEFAULT_DSP_CALLBACK_ADDRESS);
        
        extension.initialize(spyContext);
        
        verify(configurer).configure(spyContext, webServer, SETTINGS);
        var apiConfig = spyContext.getService(DspApiConfiguration.class);
        assertThat(apiConfig.getContextAlias()).isEqualTo(CONTEXT_ALIAS);
        assertThat(apiConfig.getDspCallbackAddress()).isEqualTo(DEFAULT_DSP_CALLBACK_ADDRESS);
    }
    
    @Test
    void initialize_settingsProvided_useSettings(ServiceExtensionContext context) {
        var webhookAddress = "http://webhook";
        
        var spyContext = spy(context);
        when(spyContext.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "web.http.protocol.port", String.valueOf(1234),
                "web.http.protocol.path", "/path"))
        );
        when(spyContext.getSetting(DSP_CALLBACK_ADDRESS, DEFAULT_DSP_CALLBACK_ADDRESS)).thenReturn(webhookAddress);
        
        extension.initialize(spyContext);
    
        verify(configurer).configure(spyContext, webServer, SETTINGS);
        var apiConfig = spyContext.getService(DspApiConfiguration.class);
        assertThat(apiConfig.getContextAlias()).isEqualTo(CONTEXT_ALIAS);
        assertThat(apiConfig.getDspCallbackAddress()).isEqualTo(webhookAddress);
    }

}
