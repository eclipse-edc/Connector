/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.web.spi.configuration;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class WebServiceConfigurerImplTest {


    private static final String CONFIG = "web.http.test";
    private static final String PATH = "/api";
    private static final String ALIAS = "test";
    private static final int PORT = 8080;
    private ServiceExtensionContext context;
    private WebServer server;

    private WebServiceConfigurerImpl configurator;

    @Test
    void verifyConfigure_whenDefaultConfig() {

        var config = ConfigFactory.fromMap(new HashMap<>());
        when(context.getConfig(CONFIG)).thenReturn(config);
        when(context.getMonitor()).thenReturn(new ConsoleMonitor());

        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey(CONFIG)
                .contextAlias(ALIAS)
                .defaultPath(PATH)
                .defaultPort(PORT)
                .useDefaultContext(false)
                .name("TEST")
                .build();

        var actualConfig = configurator.configure(context, server, settings);

        verify(server, times(1))
                .addPortMapping(ALIAS, PORT, PATH);

        assertThat(actualConfig.getContextAlias()).isEqualTo(ALIAS);
        assertThat(actualConfig.getPort()).isEqualTo(PORT);
        assertThat(actualConfig.getPath()).isEqualTo(PATH);

    }

    @Test
    void verifyConfigure_whenDefaultAlias() {

        var config = ConfigFactory.fromMap(new HashMap<>());
        when(context.getConfig(CONFIG)).thenReturn(config);
        String defValue = "default";
        when(server.getDefaultContextName()).thenReturn(defValue);
        when(context.getMonitor()).thenReturn(new ConsoleMonitor());

        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey(CONFIG)
                .contextAlias(ALIAS)
                .defaultPath(PATH)
                .defaultPort(PORT)
                .useDefaultContext(true)
                .name("TEST")
                .build();

        var actualConfig = configurator.configure(context, server, settings);

        verify(server, times(1))
                .getDefaultContextName();

        assertThat(actualConfig.getContextAlias()).isEqualTo(defValue);
        assertThat(actualConfig.getPort()).isEqualTo(PORT);
        assertThat(actualConfig.getPath()).isEqualTo(PATH);

    }

    @Test
    void verifyConfigure_whenExternalConfig() {

        var path = "/api/custom";
        var port = 8765;
        var apiConfig = new HashMap<String, String>();
        apiConfig.put("port", String.valueOf(port));
        apiConfig.put("path", path);

        var config = ConfigFactory.fromMap(apiConfig);
        when(context.getConfig(CONFIG)).thenReturn(config);
        when(context.getMonitor()).thenReturn(new ConsoleMonitor());

        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey(CONFIG)
                .contextAlias(ALIAS)
                .defaultPath(PATH)
                .defaultPort(PORT)
                .useDefaultContext(false)
                .name("TEST")
                .build();

        var actualConfig = configurator.configure(context, server, settings);

        verifyNoInteractions(server);
        assertThat(actualConfig.getContextAlias()).isEqualTo(ALIAS);
        assertThat(actualConfig.getPort()).isEqualTo(port);
        assertThat(actualConfig.getPath()).isEqualTo(path);

    }


    @BeforeEach
    void setup() {
        context = mock(ServiceExtensionContext.class);
        server = mock(WebServer.class);
        configurator = new WebServiceConfigurerImpl();
    }


}
