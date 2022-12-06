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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfigurationExtension.IDS_WEBHOOK_ADDRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class IdsApiConfigurationExtensionTest {

    private final WebServer webServer = mock(WebServer.class);
    private final Monitor monitor = mock(Monitor.class);
    private final WebServiceConfigurer configurator = mock(WebServiceConfigurer.class);

    private IdsApiConfigurationExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(Monitor.class, monitor);
        context.registerService(WebServer.class, webServer);
        context.registerService(WebServiceConfigurer.class, configurator);

        extension = factory.constructInstance(IdsApiConfigurationExtension.class);
    }

    @Test
    void initialize_useCurrentContextName(ServiceExtensionContext context) {
        var spyContext = spy(context);
        when(spyContext.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("web.http.protocol.port", "1111")));
        when(spyContext.getSetting(eq(IDS_WEBHOOK_ADDRESS), any())).thenReturn("http://hostname");
        var webServiceConfiguration = WebServiceConfiguration.Builder
                .newInstance().contextAlias("protocol").path("/path").port(8273).build();
        when(configurator.configure(any(), any(), any())).thenReturn(webServiceConfiguration);

        extension.initialize(spyContext);

        verify(configurator).configure(any(), eq(webServer), argThat(settings -> "protocol".equals(settings.getContextAlias())));
        var configuration = spyContext.getService(IdsApiConfiguration.class);
        assertThat(configuration.getContextAlias()).isEqualTo("protocol");
        assertThat(configuration.getIdsWebhookAddress()).isEqualTo("http://hostname/path/data");
    }

    @Test
    void initialize_shouldUseDeprecatedContextName_whenRelatedSettingsExist(ServiceExtensionContext context) {
        var spyContext = spy(context);
        var webServiceConfiguration = WebServiceConfiguration.Builder
                .newInstance().contextAlias("ids").path("/any").port(8273).build();
        when(configurator.configure(any(), any(), any())).thenReturn(webServiceConfiguration);
        when(spyContext.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("web.http.ids.port", "1111")));

        extension.initialize(spyContext);

        var configuration = spyContext.getService(IdsApiConfiguration.class);
        assertThat(configuration.getContextAlias()).isEqualTo("ids");
        verify(configurator).configure(any(), eq(webServer), argThat(settings -> "ids".equals(settings.getContextAlias())));
        verify(monitor).warning(anyString());
    }
}
