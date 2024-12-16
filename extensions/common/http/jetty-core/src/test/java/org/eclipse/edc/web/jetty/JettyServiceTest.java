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

package org.eclipse.edc.web.jetty;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JettyServiceTest {

    private final Monitor monitor = mock();
    private final PortMappingRegistry portMappingRegistry = mock();
    private final JettyConfiguration configuration = new JettyConfiguration(null, null);
    private final JettyService jettyService = new JettyService(configuration, monitor, portMappingRegistry);

    @AfterEach
    void teardown() {
        jettyService.shutdown();
    }

    @Test
    void shouldRegisterServletOnConfiguredPortMapping() {
        var portMapping = new PortMapping("context", 9191, "/path");
        when(portMappingRegistry.getAll()).thenReturn(List.of(portMapping));

        jettyService.start();
        jettyService.registerServlet("context", new TestServlet());

        given()
                .get("http://localhost:9191/path/test/resource")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldConfigureMultipleApiContexts() {
        var portMapping = new PortMapping("context", 9191, "/path");
        var anotherPortMapping = new PortMapping("another", 9292, "/another/path");
        when(portMappingRegistry.getAll()).thenReturn(List.of(portMapping, anotherPortMapping));

        jettyService.start();
        jettyService.registerServlet("context", new TestServlet());
        jettyService.registerServlet("another", new TestServlet());

        given()
                .get("http://localhost:9191/path/test/resource")
                .then()
                .statusCode(200);

        given()
                .get("http://localhost:9292/another/path/test/resource")
                .then()
                .statusCode(200);
    }

    @Test
    void verifyConnectorConfigurationCallback() {
        var listener = new JettyListener();

        when(portMappingRegistry.getAll()).thenReturn(List.of(new PortMapping("default", 7171, "/api")));
        jettyService.addConnectorConfigurationCallback((c) -> c.addBean(listener));

        jettyService.start();
        jettyService.registerServlet("default", new TestServlet());

        assertThat(listener.getConnectionsOpened()).isEqualTo(0);
        given()
                .get("http://localhost:7171/api/test/resource")
                .then()
                .statusCode(200);
        assertThat(listener.getConnectionsOpened()).isEqualTo(1);
    }

    @Test
    void verifyCustomPathRoot() {
        when(portMappingRegistry.getAll()).thenReturn(List.of(new PortMapping("default", 7171, "/")));

        jettyService.start();
        jettyService.registerServlet("default", new TestServlet());

        given()
                .get("http://localhost:7171/test/resource")
                .then()
                .statusCode(200);
    }

    private static class JettyListener extends AbstractLifeCycle implements Connection.Listener {

        private final AtomicInteger connectionsOpened = new AtomicInteger();

        @Override
        public void onOpened(Connection connection) {
            connectionsOpened.incrementAndGet();
        }

        @Override
        public void onClosed(Connection connection) {
        }

        public int getConnectionsOpened() {
            return connectionsOpened.intValue();
        }
    }

    private static class TestServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().write("{}");
        }
    }
}
