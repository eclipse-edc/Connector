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
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.edc.protocol.ids.api.configuration;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.DEFAULT_IDS_API_PATH;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.DEFAULT_IDS_PORT;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.DEFAULT_IDS_WEBHOOK_ADDRESS;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.IDS_API_CONFIG;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.IDS_API_CONTEXT_ALIAS;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.IDS_WEBHOOK_ADDRESS;
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

    private WebServiceConfigurer configurator;

    private ArgumentCaptor<String> contextNameCaptor;
    private ArgumentCaptor<Integer> portCaptor;
    private ArgumentCaptor<String> pathCaptor;
    private ArgumentCaptor<IdsApiConfiguration> apiConfigCaptor;

    @BeforeEach
    void setUp() {
        var monitor = new ConsoleMonitor();

        webServer = mock(WebServer.class);
        context = mock(ServiceExtensionContext.class);
        configurator = new WebServiceConfigurerImpl();

        when(context.getMonitor()).thenReturn(monitor);
        when(context.getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS))
                .thenReturn(DEFAULT_IDS_WEBHOOK_ADDRESS);

        extension = new IdsApiConfigurationExtension();
        contextNameCaptor = ArgumentCaptor.forClass(String.class);
        portCaptor = ArgumentCaptor.forClass(Integer.class);
        pathCaptor = ArgumentCaptor.forClass(String.class);
        apiConfigCaptor = ArgumentCaptor.forClass(IdsApiConfiguration.class);
        setField("webServer", webServer);
        setField("configurator", configurator);

    }

    @Test
    void initializeWithDefault() {
        var config = ConfigFactory.fromMap(new HashMap<>());
        when(context.getConfig(IDS_API_CONFIG)).thenReturn(config);

        extension.initialize(context);

        verify(webServer, times(1))
                .addPortMapping(contextNameCaptor.capture(), portCaptor.capture(), pathCaptor.capture());
        assertThat(contextNameCaptor.getValue()).isEqualTo(IDS_API_CONTEXT_ALIAS);
        assertThat(portCaptor.getValue()).isEqualTo(DEFAULT_IDS_PORT);
        assertThat(pathCaptor.getValue()).isEqualTo(DEFAULT_IDS_API_PATH);

        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verify(context, times(1)).getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS);
        verifyNoMoreInteractions(context);

        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getIdsWebhookAddress()).isEqualTo(DEFAULT_IDS_WEBHOOK_ADDRESS + DEFAULT_IDS_API_PATH + "/data");
    }

    @Test
    void initializeWithCustomSettings() {
        var path = "/api/ids/custom";
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("port", String.valueOf(8765));
        apiConfig.put("path", path);

        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(IDS_API_CONFIG)).thenReturn(config);

        var address = "http://somehost:1234";
        when(context.getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS))
                .thenReturn(address);

        extension.initialize(context);

        verifyNoInteractions(webServer);

        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verify(context, times(1)).getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS);
        verifyNoMoreInteractions(context);

        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getIdsWebhookAddress()).isEqualTo(address + path + "/data");
    }

    @Test
    void initializeWithOnlyPortSet() {
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("port", String.valueOf(8765));

        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(IDS_API_CONFIG)).thenReturn(config);

        extension.initialize(context);

        verifyNoInteractions(webServer);

        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verify(context, times(1)).getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS);
        verifyNoMoreInteractions(context);

        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getIdsWebhookAddress()).isEqualTo(DEFAULT_IDS_WEBHOOK_ADDRESS + DEFAULT_IDS_API_PATH + "/data");
    }

    @Test
    void initializeWithOnlyPathSet() {
        var path = "/api/ids/custom";
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("path", path);

        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(IDS_API_CONFIG)).thenReturn(config);

        extension.initialize(context);

        verifyNoInteractions(webServer);

        verify(context, times(1)).getMonitor();
        verify(context, times(1)).getConfig(IDS_API_CONFIG);
        verify(context, times(1)).registerService(eq(IdsApiConfiguration.class), apiConfigCaptor.capture());
        verify(context, times(1)).getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS);
        verifyNoMoreInteractions(context);

        var idsApiConfig = apiConfigCaptor.getValue();
        assertThat(idsApiConfig.getContextAlias()).isEqualTo("ids");
        assertThat(idsApiConfig.getIdsWebhookAddress()).isEqualTo(DEFAULT_IDS_WEBHOOK_ADDRESS + path + "/data");
    }

    private void setField(String field, Object value) {
        try {
            Field webServerField = IdsApiConfigurationExtension.class.getDeclaredField(field);
            webServerField.setAccessible(true);
            webServerField.set(extension, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //
        }
    }


}
