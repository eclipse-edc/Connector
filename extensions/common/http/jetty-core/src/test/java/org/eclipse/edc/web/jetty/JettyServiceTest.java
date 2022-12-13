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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JettyServiceTest {

    private JettyService jettyService;
    private Monitor monitor;
    private TestController testController;

    @BeforeEach
    void setUp() {
        monitor = mock(Monitor.class);
        testController = new TestController();
    }

    @Test
    void verifyDefaultPortMapping() {
        var config = ConfigFactory.fromMap(Map.of("web.http.port", "7171")); //default port mapping
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        jettyService.start();

        var servletContainer = new ServletContainer(createTestResource());
        jettyService.registerServlet("default", servletContainer);

        given()
                .get("http://localhost:7171/api/test/resource")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Verifies a custom port mapping")
    void verifyCustomPortMapping() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.another.port", "9191",
                "web.http.another.path", "/another")); //default port mapping
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);
        ResourceConfig rc = createTestResource();

        jettyService.start();

        jettyService.registerServlet("another", new ServletContainer(rc));

        given()
                .get("http://localhost:9191/another/test/resource")
                .then()
                .statusCode(200);

        //verify that there is no default port mapping anymore
        assertThatThrownBy(() -> given().get("http://localhost:8872/api/test/resource").then())
                .isInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("Verifies that a custom port mapping and the implicit default mapping is possible")
    void verifyDefaultAndCustomPortMapping() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.port", "7171",
                "web.http.another.port", "9191",
                "web.http.another.path", "/another")); //default port mapping
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        jettyService.start();

        jettyService.registerServlet("another", new ServletContainer(createTestResource()));
        jettyService.registerServlet("default", new ServletContainer(createTestResource()));

        given()
                .get("http://localhost:9191/another/test/resource")
                .then()
                .statusCode(200);

        given()
                .get("http://localhost:7171/api/test/resource")
                .then()
                .statusCode(200);
    }

    @Test
    void verifyConnectorConfigurationCallback() {
        var listener = new JettyListener();

        var config = ConfigFactory.fromMap(Map.of("web.http.port", "7171"));
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);
        jettyService.addConnectorConfigurationCallback((c) -> c.addBean(listener));

        jettyService.start();

        jettyService.registerServlet("default", new ServletContainer(createTestResource()));

        assertThat(listener.getConnectionsOpened()).isEqualTo(0);
        given()
                .get("http://localhost:7171/api/test/resource")
                .then()
                .statusCode(200);
        assertThat(listener.getConnectionsOpened()).isEqualTo(1);
    }

    @Test
    void verifyCustomPathRoot() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.port", "7171",
                "web.http.path", "/"));
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        jettyService.start();

        jettyService.registerServlet("default", new ServletContainer(createTestResource()));

        given()
                .get("http://localhost:7171/test/resource")
                .then()
                .statusCode(200);
    }

    @Test
    void verifyInvalidPathSpecThrowsException() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.port", "7171",
                "web.http.another.port", "9191",
                "web.http.another.path", "another")); //misses leading slash
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        assertThatThrownBy(() -> jettyService.start()).isInstanceOf(EdcException.class)
                .hasMessage("Error starting Jetty service")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("A context path must start with /: another");
    }

    @Test
    void verifyIdenticalPorts_shouldThrowException() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.first.port", "7171",
                "web.http.first.path", "/first",
                "web.http.another.port", "7171",
                "web.http.another.path", "/another"));
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        assertThatThrownBy(() -> jettyService.start()).isInstanceOf(EdcException.class)
                .hasMessage("Error starting Jetty service")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("A binding for port 7171 already exists");
    }

    @AfterEach
    void teardown() {
        jettyService.shutdown();
    }

    @NotNull
    private ResourceConfig createTestResource() {
        var rc = new ResourceConfig();
        rc.registerClasses(TestController.class);
        rc.registerInstances(new TestBinder());
        return rc;
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Path("/test")
    public static class TestController { //needs to be public, otherwise it won't get picked up

        @GET
        @Path("/resource")
        public String foo() {
            return "exists";
        }
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

    /**
     * Maps (JAX-RS resource) instances to types.
     */
    private class TestBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(testController).to(TestController.class);
        }
    }
}
