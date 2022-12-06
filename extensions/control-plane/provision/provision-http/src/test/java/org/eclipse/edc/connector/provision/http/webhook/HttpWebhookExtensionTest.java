/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.http.webhook;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.provision.http.HttpProvisionerWebhookUrl;
import org.eclipse.edc.connector.provision.http.HttpWebhookExtension;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HttpWebhookExtensionTest {

    private HttpWebhookExtension extension;
    private ServiceExtensionContext context;
    private final WebServer webServer = mock(WebServer.class);
    private final WebService webService = mock(WebService.class);
    private final Monitor monitor = mock(Monitor.class);
    private final WebServiceConfigurer webServiceConfigurer = mock(WebServiceConfigurer.class);

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        var webServiceConfiguration = WebServiceConfiguration.Builder.newInstance()
                .contextAlias("management")
                .path("/management")
                .port(8888)
                .build();
        context.registerService(WebServer.class, webServer);
        context.registerService(WebService.class, webService);
        context.registerService(Hostname.class, () -> "localhost");
        context.registerService(TransferProcessService.class, mock(TransferProcessService.class));
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));
        context.registerService(WebServiceConfigurer.class, webServiceConfigurer);
        context.registerService(ManagementApiConfiguration.class, new ManagementApiConfiguration(webServiceConfiguration));

        this.context = spy(context); //used to inject the config
        when(this.context.getMonitor()).thenReturn(monitor);

        extension = factory.constructInstance(HttpWebhookExtension.class);
    }

    @Test
    void initialize_shouldBeRegisteredAsManagementApiService() {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(webService).registerResource(eq("management"), isA(HttpProvisionerWebhookApiController.class));
        verify(webService).registerResource(eq("management"), isA(AuthenticationRequestFilter.class));
        assertThat(context.getService(HttpProvisionerWebhookUrl.class))
                .extracting(HttpProvisionerWebhookUrl::get).extracting(URL::toString)
                .isEqualTo("http://localhost:8888/management/callback");
        verifyNoInteractions(webServiceConfigurer);
        verifyNoMoreInteractions(webServer, webService);
    }

    @Test
    void initialize_shouldUseDeprecatedSettings_whenProvisionerSettingsGroupExists() {
        var config = ConfigFactory.fromMap(Map.of("web.http.provisioner.path", "/api/v1/someotherpath/webhook"));
        when(context.getConfig()).thenReturn(config);
        when(context.getConfig("web.http.provisioner")).thenReturn(config.getConfig("web.http.provisioner"));
        var webServiceConfiguration = WebServiceConfiguration.Builder.newInstance().contextAlias("provisioner").port(9999).path("/any").build();
        when(webServiceConfigurer.configure(any(), any(), any())).thenReturn(webServiceConfiguration);

        extension.initialize(context);

        verify(webService).registerResource(eq("provisioner"), isA(HttpProvisionerWebhookApiController.class));
        verify(webService).registerResource(eq("provisioner"), isA(AuthenticationRequestFilter.class));
        verify(monitor).warning(anyString());
    }
}
