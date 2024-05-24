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

package org.eclipse.edc.connector.controlplane.provision.http.webhook;

import org.eclipse.edc.connector.controlplane.provision.http.HttpProvisionerWebhookUrl;
import org.eclipse.edc.connector.controlplane.provision.http.HttpWebhookExtension;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.context.ManagementApiUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HttpWebhookExtensionTest {

    private final WebService webService = mock(WebService.class);

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(Hostname.class, () -> "localhost");
        context.registerService(TransferProcessService.class, mock(TransferProcessService.class));
        ManagementApiUrl managementApiUrl = () -> URI.create("http://localhost:8888/management");
        context.registerService(ManagementApiUrl.class, managementApiUrl);
    }

    @Test
    void initialize_shouldBeRegisteredAsManagementApiService(HttpWebhookExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(webService).registerResource(eq("management"), isA(HttpProvisionerWebhookApiController.class));
        assertThat(context.getService(HttpProvisionerWebhookUrl.class))
                .extracting(HttpProvisionerWebhookUrl::get).extracting(URL::toString)
                .isEqualTo("http://localhost:8888/management/callback");
    }
}
