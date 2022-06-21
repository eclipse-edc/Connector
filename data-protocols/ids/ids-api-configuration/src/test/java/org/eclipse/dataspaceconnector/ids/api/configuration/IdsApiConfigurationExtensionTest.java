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

import org.eclipse.dataspaceconnector.spi.WebServer;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IdsApiConfigurationExtensionTest {
    
    private WebServer webServer;
    private ServiceExtensionContext context;
    private IdsApiConfigurationExtension extension;
    
    private ArgumentCaptor<String> contextNameCaptor;
    private ArgumentCaptor<Integer> portCaptor;
    private ArgumentCaptor<String> pathCaptor;
    private ArgumentCaptor<IdsApiConfiguration> apiConfigCaptor;
    
    @BeforeEach
    void setUp() {
        var monitor = new ConsoleMonitor();
        webServer = mock(WebServer.class);
        context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(monitor);
        
        extension = new IdsApiConfigurationExtension();
        contextNameCaptor = ArgumentCaptor.forClass(String.class);
        portCaptor = ArgumentCaptor.forClass(Integer.class);
        pathCaptor = ArgumentCaptor.forClass(String.class);
        apiConfigCaptor = ArgumentCaptor.forClass(IdsApiConfiguration.class);
        setJettyService();
    }
    
    @Test
    void initializeWithDefault() {
        var config = ConfigFactory.fromMap(new HashMap<>());
        when(context.getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG)).thenReturn(config);
        
        extension.initialize(context);
        
        verify(webServer, times(1))
                .addPortMapping(contextNameCaptor.capture(), portCaptor.capture(), pathCaptor.capture());
        assertThat(contextNameCaptor.getValue()).isEqualTo(IdsApiConfigurationExtension.IDS_API_CONTEXT_ALIAS);
        assertThat(portCaptor.getValue()).isEqualTo(IdsApiConfigurationExtension.DEFAULT_IDS_PORT);
        assertThat(pathCaptor.getValue()).isEqualTo(IdsApiConfigurationExtension.DEFAULT_IDS_API_PATH);
    
        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verifyNoMoreInteractions(context);
        
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
    
        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG)).thenReturn(config);
        
        extension.initialize(context);
        
        verifyNoInteractions(webServer);
    
        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verifyNoMoreInteractions(context);
        
        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getPath()).isEqualTo(path);
    }
    
    @Test
    void initializeWithOnlyPortSet() {
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("port", String.valueOf(8765));
    
        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG)).thenReturn(config);
    
        extension.initialize(context);
    
        verifyNoInteractions(webServer);
    
        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verifyNoMoreInteractions(context);
    
        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getPath()).isEqualTo(IdsApiConfigurationExtension.DEFAULT_IDS_API_PATH);
    }
    
    @Test
    void initializeWithOnlyPathSet() {
        var path = "/api/ids/custom";
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("path", path);
    
        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG)).thenReturn(config);
    
        extension.initialize(context);
    
        verifyNoInteractions(webServer);
    
        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IdsApiConfigurationExtension.IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verifyNoMoreInteractions(context);
    
        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getPath()).isEqualTo(path);
    }
    
    private void setJettyService() {
        try {
            Field webServerField = IdsApiConfigurationExtension.class.getDeclaredField("webServer");
            webServerField.setAccessible(true);
            webServerField.set(extension, webServer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //
        }
    }
}
