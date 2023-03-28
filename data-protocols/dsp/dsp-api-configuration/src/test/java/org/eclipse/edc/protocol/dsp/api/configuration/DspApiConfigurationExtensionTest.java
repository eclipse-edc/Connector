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

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurerImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfigurationExtension.DEFAULT_PROTOCOL_API_PATH;
import static org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfigurationExtension.DEFAULT_PROTOCOL_PORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DspApiConfigurationExtensionTest {
    
    private DspApiConfigurationExtension extension;
    private WebServiceConfigurer configurer = spy(WebServiceConfigurerImpl.class);
    private WebServer webServer = mock(WebServer.class);
    private final Monitor monitor = mock(Monitor.class);
    
    private ResultCaptor captor;
    
    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(WebService.class, mock(WebService.class));
        context.registerService(WebServer.class, webServer);
        context.registerService(WebServiceConfigurer.class, configurer);
        context.registerService(IdentityService.class, mock(IdentityService.class));
        extension = factory.constructInstance(DspApiConfigurationExtension.class);
    
        captor = new ResultCaptor();
        doAnswer(captor).when(configurer).configure(any(), any(), any());
    }
    
    @Test
    void initialize_noSettingsProvided_useDspDefault() {
        var context = contextWithConfig(ConfigFactory.empty());
        
        extension.initialize(context);
        
        verify(webServer).addPortMapping("protocol", DEFAULT_PROTOCOL_PORT, DEFAULT_PROTOCOL_API_PATH);
        var apiConfig = captor.getConfiguration();
        assertThat(apiConfig.getPort()).isEqualTo(DEFAULT_PROTOCOL_PORT);
        assertThat(apiConfig.getPath()).isEqualTo(DEFAULT_PROTOCOL_API_PATH);
    }
    
    @Test
    void initialize_settingsProvided_useSettings() {
        var port = 9292;
        var path = "/path";
        var config = Map.of("web.http.protocol.port", String.valueOf(port), "web.http.protocol.path", path);
        var context = contextWithConfig(ConfigFactory.fromMap(config));
    
        extension.initialize(context);
    
        verify(webServer, never()).addPortMapping(anyString(), anyInt(), anyString());
        var apiConfig = captor.getConfiguration();
        assertThat(apiConfig.getPort()).isEqualTo(port);
        assertThat(apiConfig.getPath()).isEqualTo(path);
    }
    
    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, List.of(() -> config));
        context.initialize();
        return context;
    }
    
    /**
     * Captures the WebServiceConfiguration returned by the WebServiceConfigurator.
     */
    private static class ResultCaptor implements Answer<WebServiceConfiguration> {
        private WebServiceConfiguration configuration;
        
        public WebServiceConfiguration getConfiguration() {
            return configuration;
        }
    
        @Override
        public WebServiceConfiguration answer(InvocationOnMock invocation) throws Throwable {
            configuration = (WebServiceConfiguration) invocation.callRealMethod();
            return configuration;
        }
    }
}
