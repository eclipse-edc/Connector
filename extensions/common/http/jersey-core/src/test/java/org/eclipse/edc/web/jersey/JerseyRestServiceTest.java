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

package org.eclipse.edc.web.jersey;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.jetty.JettyConfiguration;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.edc.web.jetty.PortMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class JerseyRestServiceTest {
    private final int httpPort = getFreePort();
    private final Monitor monitor = mock(Monitor.class);
    private JerseyRestService jerseyRestService;
    private JettyService jettyService;

    @AfterEach
    void teardown() {
        jettyService.shutdown();
    }

    @Test
    @DisplayName("Verifies that a resource is available under the default path")
    void verifyDefaultContextPath() {
        startJetty(PortMapping.getDefault(httpPort));
        jerseyRestService.registerResource(new TestController());
        jerseyRestService.start();

        given()
                .get("http://localhost:" + httpPort + "/api/test/resource")
                .then()
                .statusCode(200)
                .body(is("exists"));
    }

    @Test
    @DisplayName("Verifies that a second resource is available under a specific path and port")
    void verifyAnotherContextPath() {
        var anotherPort = getFreePort();
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("path", anotherPort, "/path")
        );
        var pathController = spy(new TestController());
        var defaultController = spy(new TestController());
        jerseyRestService.registerResource("path", pathController);
        jerseyRestService.registerResource(defaultController);
        jerseyRestService.start();

        given()
                .get("http://localhost:" + anotherPort + "/path/test/resource")
                .then()
                .statusCode(200)
                .body(is("exists"));

        verify(pathController).foo();
        verifyNoInteractions(defaultController);

        given()
                .get("http://localhost:" + httpPort + "/api/test/resource")
                .then()
                .statusCode(200)
                .body(is("exists"));

        verifyNoMoreInteractions(pathController);
        verify(defaultController).foo();
    }

    @Test
    @DisplayName("Verifies that registering two port mappings under the same path throws an exception")
    void verifyIdenticalContextPats_throwsException() {
        var port1 = getFreePort();
        var port2 = getFreePort();
        startJetty(new PortMapping("path1", port1, "/path"),
                new PortMapping("path2", port2, "/path"));

        jerseyRestService.registerResource("path1", new TestController());
        jerseyRestService.registerResource("path2", new TestController());

        assertThatThrownBy(() -> jerseyRestService.start()).isInstanceOf(EdcException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verifies that a request filter only fires for the desired path/context")
    void verifyFilterForOneContextPath() throws IOException {
        var anotherPort = getFreePort();
        var filterMock = mock(ContainerRequestFilter.class);
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("path", anotherPort, "/path")
        );

        jerseyRestService.registerResource(new TestController());
        jerseyRestService.registerResource("path", new TestController());
        jerseyRestService.registerResource("path", filterMock);
        jerseyRestService.start();

        //verify that the first request hits the filter
        given()
                .get("http://localhost:" + anotherPort + "/path/test/resource")
                .then()
                .statusCode(200);
        verify(filterMock).filter(any(ContainerRequestContext.class));
        verifyNoMoreInteractions(filterMock);

        //verify that the second request does NOT hit the filter
        reset(filterMock);
        given()
                .get("http://localhost:" + httpPort + "/api/test/resource")
                .then()
                .statusCode(200);

        verifyNoInteractions(filterMock);
    }

    @Test
    @DisplayName("Verifies that different filters fire for different paths")
    void verifySeparateFilters() {
        var port1 = getFreePort();
        var port2 = getFreePort();
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("foo", port1, "/foo"),
                new PortMapping("bar", port2, "/bar")
        );
        // mocking the ContextRequestFilter doesn't work here, Mockito apparently re-uses mocks for the same target class
        var barFilter = mock(BarRequestFilter.class);
        var fooRequestFilter = mock(FooRequestFilter.class);
        jerseyRestService.registerResource("foo", new TestController());
        jerseyRestService.registerResource("foo", fooRequestFilter);
        jerseyRestService.registerResource("bar", new TestController());
        jerseyRestService.registerResource("bar", barFilter);
        jerseyRestService.start();

        //verify that the first request hits only the bar filter
        given()
                .get("http://localhost:" + port2 + "/bar/test/resource")
                .then()
                .statusCode(200);

        verify(fooRequestFilter, never()).filter(any(ContainerRequestContext.class));
        verify(barFilter).filter(any(ContainerRequestContext.class));
        verifyNoMoreInteractions(barFilter);

        reset(barFilter, fooRequestFilter);

        //  verify that the second request only hits the foo filter
        given()
                .get("http://localhost:" + port1 + "/foo/test/resource")
                .then()
                .statusCode(200);

        verify(barFilter, never()).filter(any());
        verify(fooRequestFilter).filter(any());
        verifyNoMoreInteractions(fooRequestFilter);
    }

    @Test
    @DisplayName("Verifies that different filters fire for different controllers")
    void verifySeparateFiltersForDifferentControllers() {
        var port1 = getFreePort();
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("foo", port1, "/foo")
        );
        // mocking the ContextRequestFilter doesn't work here, Mockito apparently re-uses mocks for the same target class
        var testControllerFilter = mock(BarRequestFilter.class);
        var bazControllerFilter = mock(FooRequestFilter.class);

        jerseyRestService.registerResource("foo", new TestController());
        jerseyRestService.registerResource("foo", new BazController());
        jerseyRestService.registerDynamicResource("foo", TestController.class, testControllerFilter);
        jerseyRestService.registerDynamicResource("foo", BazController.class, bazControllerFilter);
        jerseyRestService.start();

        //verify that the first request hits only the TestController filter
        given()
                .get("http://localhost:" + port1 + "/foo/test/resource")
                .then()
                .statusCode(200);

        verify(bazControllerFilter, never()).filter(any(ContainerRequestContext.class));
        verify(testControllerFilter).filter(any(ContainerRequestContext.class));
        verifyNoMoreInteractions(testControllerFilter);

        reset(bazControllerFilter, testControllerFilter);

        //  verify that the second request only hits the BazController filter
        given()
                .get("http://localhost:" + port1 + "/foo/baz/resource")
                .then()
                .statusCode(200);

        verify(testControllerFilter, never()).filter(any());
        verify(bazControllerFilter).filter(any());
        verifyNoMoreInteractions(bazControllerFilter);
    }

    @Test
    @DisplayName("Verifies that registering two identical paths raises an exception")
    void verifyIdenticalPathsRaiseException() {
        var port1 = getFreePort();
        var port2 = getFreePort();
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("another", port1, "/foo"),
                new PortMapping("yet-another", port2, "/foo")
        );

        jerseyRestService.registerResource("another", new TestController());
        jerseyRestService.registerResource("yet-another", new TestController());

        assertThatThrownBy(() -> jerseyRestService.start()).isInstanceOf(EdcException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verifies that registering a non-existing context alias raises an exception")
    void verifyInvalidContextAlias_shouldThrowException() {
        var anotherPort = getFreePort();
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("another", anotherPort, "/foo")
        );

        jerseyRestService.registerResource("not-exists", new TestController());

        assertThatThrownBy(() -> jerseyRestService.start()).isInstanceOf(EdcException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    private void startJetty(PortMapping... mapping) {
        var config = new JettyConfiguration(null, null);
        Arrays.stream(mapping).forEach(config::portMapping);
        jettyService = new JettyService(config, monitor);
        jerseyRestService = new JerseyRestService(jettyService, new JacksonTypeManager(), JerseyConfiguration.Builder.newInstance().build(), monitor);
        jettyService.start();
    }

    //needs to be public, otherwise it won't get picked up
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/test")
    public static class TestController { //needs to be public, otherwise it won't get picked up

        @GET
        @Path("/resource")
        public String foo() {
            return "exists";
        }
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Path("/baz")
    public static class BazController { //needs to be public, otherwise it won't get picked up

        @GET
        @Path("/resource")
        public String foo() {
            return "exists";
        }
    }

    //needs to be public, otherwise it won't get picked up
    public static class BarRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) {

        }
    }

    //needs to be public, otherwise it won't get picked up
    public static class FooRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) {

        }
    }
}
