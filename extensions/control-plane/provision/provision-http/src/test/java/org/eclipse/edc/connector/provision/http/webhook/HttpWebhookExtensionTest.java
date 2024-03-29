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
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.controlplane.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.provision.http.HttpProvisionerWebhookUrl;
import org.eclipse.edc.connector.provision.http.HttpWebhookExtension;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HttpWebhookExtensionTest {

    private HttpWebhookExtension extension;
    private final WebService webService = mock(WebService.class);
    private final Monitor monitor = mock(Monitor.class);

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        var webServiceConfiguration = WebServiceConfiguration.Builder.newInstance()
                .contextAlias("management")
                .path("/management")
                .port(8888)
                .build();
        context.registerService(WebService.class, webService);
        context.registerService(Hostname.class, () -> "localhost");
        context.registerService(TransferProcessService.class, mock(TransferProcessService.class));
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));
        context.registerService(ManagementApiConfiguration.class, new ManagementApiConfiguration(webServiceConfiguration));

        when(context.getMonitor()).thenReturn(monitor);

        extension = factory.constructInstance(HttpWebhookExtension.class);
    }

    @Test
    void initialize_shouldBeRegisteredAsManagementApiService(ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(webService).registerResource(eq("management"), isA(HttpProvisionerWebhookApiController.class));
        verify(webService).registerResource(eq("management"), isA(AuthenticationRequestFilter.class));
        assertThat(context.getService(HttpProvisionerWebhookUrl.class))
                .extracting(HttpProvisionerWebhookUrl::get).extracting(URL::toString)
                .isEqualTo("http://localhost:8888/management/callback");
    }
}
