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

package org.eclipse.edc.web.jersey.testfixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.JerseyConfiguration;
import org.eclipse.edc.web.jersey.JerseyRestService;
import org.eclipse.edc.web.jersey.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.jetty.JettyConfiguration;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.edc.web.jetty.PortMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.Mockito.mock;

/**
 * Base utility class that permits to test Rest controllers deploying a bare bone instance of Jetty
 * with Jersey. The controller returned by the {@link #controller()} method gets registered on a test api context.
 */
public abstract class RestControllerTestBase {

    protected final int port = getFreePort();
    protected final Monitor monitor = mock(Monitor.class);
    protected final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private JettyService jetty;

    @BeforeEach
    final void startJetty() {
        var config = new JettyConfiguration(null, null);
        config.portMapping(new PortMapping("test", port, "/"));
        jetty = new JettyService(config, monitor);
        var jerseyService = new JerseyRestService(jetty, new TypeManager(), mock(JerseyConfiguration.class), monitor);
        jerseyService.registerResource("test", new ObjectMapperProvider(objectMapper));
        jerseyService.registerResource("test", controller());
        Object additionalResource = additionalResource();
        if (additionalResource != null) {
            jerseyService.registerResource("test", additionalResource);
        }
        jetty.start();

        jerseyService.start();
    }

    @AfterEach
    void teardown() {
        jetty.shutdown();
    }

    protected abstract Object controller();

    protected Object additionalResource() {
        return null;
    }

}
