/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.ids.api.configuration;

import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.extension.jetty.PortMapping;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class IdsApiConfigurationExtensionTest {
    
    private JettyService jettyService;
    private ServiceExtensionContext context;
    private Config config;
    private IdsApiConfigurationExtension extension;
    
    private ArgumentCaptor<PortMapping> portMappingCaptor;
    private ArgumentCaptor<IdsApiConfiguration> apiConfigCaptor;
    
    @BeforeEach
    void setUp() {
        var monitor = new ConsoleMonitor();
        jettyService = mock(JettyService.class);
        context = mock(ServiceExtensionContext.class);
        config = mock(Config.class);
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG)).thenReturn(config);
        
        extension = new IdsApiConfigurationExtension();
        portMappingCaptor = ArgumentCaptor.forClass(PortMapping.class);
        apiConfigCaptor = ArgumentCaptor.forClass(IdsApiConfiguration.class);
        setJettyService();
    }
    
    @Test
    void initializeWithDefault() {
        when(config.getEntries()).thenReturn(new HashMap<>());
        
        extension.initialize(context);
        
        verify(jettyService, times(1)).addPortMapping(portMappingCaptor.capture());
        var portMapping = portMappingCaptor.getValue();
        assertThat(portMapping.getPort()).isEqualTo(IdsApiConfigurationExtension.DEFAULT_IDS_PORT);
        assertThat(portMapping.getPath()).isEqualTo(IdsApiConfigurationExtension.DEFAULT_IDS_API_PATH);
        
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getPath()).isEqualTo(IdsApiConfigurationExtension.DEFAULT_IDS_API_PATH);
    }
    
    @Test
    void initializeWithCustomSettings() {
        var path = "/api/ids/custom";
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("port", String.valueOf(8765));
        apiConfig.put("path", path);
        
        when(config.getEntries()).thenReturn(apiConfig);
        when(config.getString(eq("path"), any())).thenReturn(path);
        
        extension.initialize(context);
        
        verifyNoInteractions(jettyService);
    
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getPath()).isEqualTo(path);
    }
    
    private void setJettyService() {
        try {
            Field jettyServiceField = IdsApiConfigurationExtension.class.getDeclaredField("jettyService");
            jettyServiceField.setAccessible(true);
            jettyServiceField.set(extension, jettyService);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //
        }
    }
}
