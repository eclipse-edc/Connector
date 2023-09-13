/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.control.configuration;

import org.eclipse.edc.connector.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.api.control.configuration.ControlApiConfigurationExtension.CONTROL_API_ENDPOINT;
import static org.eclipse.edc.connector.api.control.configuration.ControlApiConfigurationExtension.CONTROL_CONTEXT_ALIAS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class ControlApiConfigurationExtensionTest {

    private final WebServiceConfigurer configurator = mock();

    private final WebServiceConfiguration webServiceConfiguration = WebServiceConfiguration.Builder.newInstance()
            .contextAlias(CONTROL_CONTEXT_ALIAS)
            .path("/path")
            .port(1234)
            .build();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebServiceConfigurer.class, configurator);
        context.registerService(Hostname.class, () -> "localhost");

        when(configurator.configure(any(), any(), any())).thenReturn(webServiceConfiguration);
    }

    @Test
    void initialize(ControlApiConfigurationExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        var config = context.getService(ControlApiConfiguration.class);
        assertThat(config.getContextAlias()).isEqualTo(webServiceConfiguration.getContextAlias());
        assertThat(config.getPath()).isEqualTo(webServiceConfiguration.getPath());
        assertThat(config.getPort()).isEqualTo(webServiceConfiguration.getPort());

        var url = context.getService(ControlApiUrl.class);
        assertThat(url.get().toString()).isEqualTo(format("http://localhost:%s%s", config.getPort(), config.getPath()));
    }

    @Test
    void initialize_withValidEndpoint(ControlApiConfigurationExtension extension, ServiceExtensionContext context) {
        var endpoint = "http://localhost:8080/test";
        when(context.getConfig()).thenReturn(ConfigFactory.empty());
        when(context.getSetting(eq(CONTROL_API_ENDPOINT), any())).thenReturn(endpoint);

        extension.initialize(context);

        var config = context.getService(ControlApiConfiguration.class);
        assertThat(config.getContextAlias()).isEqualTo(webServiceConfiguration.getContextAlias());
        assertThat(config.getPath()).isEqualTo(webServiceConfiguration.getPath());
        assertThat(config.getPort()).isEqualTo(webServiceConfiguration.getPort());

        var url = context.getService(ControlApiUrl.class);
        assertThat(url.get().toString()).isEqualTo(endpoint);
    }

    @Test
    void initialize_withInvalidEndpoint(ControlApiConfigurationExtension extension, ServiceExtensionContext context) {
        var endpoint = "http:// invalid";
        when(context.getConfig()).thenReturn(ConfigFactory.empty());
        when(context.getSetting(eq(CONTROL_API_ENDPOINT), any())).thenReturn(endpoint);

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(EdcException.class);

    }
}
