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

package org.eclipse.dataspaceconnector.extension.jetty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.core.config.ConfigFactory;
import org.eclipse.dataspaceconnector.core.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JettyPortMappingTest {
    private JettyService jettyService;
    private Monitor monitor;
    private TestController testController;

    @BeforeEach
    void setUp() {
        monitor = new ConsoleMonitor();
        testController = new TestController();
    }

    @Test
    void verifyDefaultPortMapping() {
        var config = ConfigFactory.fromMap(Map.of("web.http.port", "7171")); //default port mapping
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        jettyService.start();

        var servletContainer = new ServletContainer(createTestResource());
        jettyService.registerServlet("default", servletContainer);

        var response = executeRequest("http://localhost:7171/api/test/resource");
        assertThat(response.code()).isEqualTo(200);
    }

    @Test
    @DisplayName("Verifies the a custom port mapping")
    void verifyCustomPortMapping() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.another.port", "9191",
                "web.http.another.name", "another",
                "web.http.another.path", "/another")); //default port mapping
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);
        ResourceConfig rc = createTestResource();

        jettyService.start();

        jettyService.registerServlet("another", new ServletContainer(rc));

        assertThat(executeRequest("http://localhost:9191/another/test/resource").code()).isEqualTo(200);
        //verify that there is no default port mapping anymore
        assertThatThrownBy(() -> executeRequest("http://localhost:8872/api/test/resource")).hasRootCauseInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("Verifies that a custom port mapping and the implicit default mapping is possible")
    void verifyDefaultAndCustomPortMapping() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.port", "7171",
                "web.http.another.port", "9191",
                "web.http.another.name", "another",
                "web.http.another.path", "/another")); //default port mapping
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        jettyService.start();

        jettyService.registerServlet("another", new ServletContainer(createTestResource()));
        jettyService.registerServlet("default", new ServletContainer(createTestResource()));

        assertThat(executeRequest("http://localhost:9191/another/test/resource").code()).isEqualTo(200);
        assertThat(executeRequest("http://localhost:7171/api/test/resource").code()).isEqualTo(200);
    }

    @Test
    @DisplayName("Verifies that an invalid path spec causes 404")
    void verifyInvalidPathSpecCauses404() {
        var config = ConfigFactory.fromMap(Map.of(
                "web.http.port", "7171",
                "web.http.another.port", "9191",
                "web.http.another.name", "another",
                "web.http.another.path", "another")); //misses leading slash
        jettyService = new JettyService(JettyConfiguration.createFromConfig(null, null, config), monitor);

        jettyService.start();

        jettyService.registerServlet("another", new ServletContainer(createTestResource()));

        assertThat(executeRequest("http://localhost:9191/another/test/resource").code()).isEqualTo(404);
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

    @NotNull
    private Response executeRequest(String url) {

        try {
            var client = new OkHttpClient.Builder().build();
            var rq = new Request.Builder().url(url).build();
            Response response = null;
            response = client.newCall(rq).execute();
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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