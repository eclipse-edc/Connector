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

package org.eclipse.dataspaceconnector.extension.jersey;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.extension.jetty.JettyConfiguration;
import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.extension.jetty.PortMapping;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class JerseyRestServiceTest {
    private final int httpPort = getFreePort();
    private JerseyRestService jerseyRestService;
    private JettyService jettyService;
    private Monitor monitorMock;

    @BeforeEach
    void setup() {
        monitorMock = mock(Monitor.class);
    }

    @Test
    @DisplayName("Verifies that a resource is available under the default path")
    void verifyDefaultContextPath() throws IOException {
        startJetty(PortMapping.getDefault(httpPort));
        jerseyRestService.registerResource(new TestController());
        jerseyRestService.start();

        var response = executeRequest("http://localhost:" + httpPort + "/api/test/resource");

        assertThat(response.code()).isEqualTo(200);
        var body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("exists");
    }

    @Test
    @DisplayName("Verifies that a second resource is available under a specific path and port")
    void verifyAnotherContextPath() throws IOException {
        var anotherPort = getFreePort();
        startJetty(
                PortMapping.getDefault(httpPort),
                new PortMapping("path", anotherPort, "/path")
        );
        jerseyRestService.registerResource("path", new TestController());
        jerseyRestService.registerResource(new TestController());
        jerseyRestService.start();


        var response = executeRequest("http://localhost:" + anotherPort + "/path/test/resource");

        assertThat(response.code()).isEqualTo(200);
        var body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("exists");


        var response2 = executeRequest("http://localhost:" + httpPort + "/api/test/resource");

        assertThat(response2.code()).isEqualTo(200);
        var body2 = response2.body();
        assertThat(body2).isNotNull();
        assertThat(body2.string()).isEqualTo("exists");
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
        assertThatThrownBy(() -> jerseyRestService.start()).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(IllegalStateException.class);
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
        var response = executeRequest("http://localhost:" + anotherPort + "/path/test/resource");

        assertThat(response.code()).isEqualTo(200);
        verify(filterMock).filter(any(ContainerRequestContext.class));
        verifyNoMoreInteractions(filterMock);

        //verify that the second request does NOT hit the filter
        reset(filterMock);
        var response2 = executeRequest("http://localhost:" + httpPort + "/api/test/resource");

        assertThat(response2.code()).isEqualTo(200);
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
        var response = executeRequest("http://localhost:" + port2 + "/bar/test/resource");

        assertThat(response.code()).isEqualTo(200);
        verify(fooRequestFilter, never()).filter(any(ContainerRequestContext.class));
        verify(barFilter).filter(any(ContainerRequestContext.class));
        verifyNoMoreInteractions(barFilter);

        reset(barFilter, fooRequestFilter);

        //  verify that the second request only hits the foo filter
        var response2 = executeRequest("http://localhost:" + port1 + "/foo/test/resource");
        assertThat(response2.code()).isEqualTo(200);
        verify(barFilter, never()).filter(any());
        verify(fooRequestFilter).filter(any());
        verifyNoMoreInteractions(fooRequestFilter);
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

    @AfterEach
    void teardown() {
        jettyService.shutdown();
    }

    private void startJetty(PortMapping... mapping) {
        JettyConfiguration config = new JettyConfiguration(null, null);
        Arrays.stream(mapping).forEach(config::portMapping);
        jettyService = new JettyService(config, monitorMock);
        jerseyRestService = new JerseyRestService(jettyService, new TypeManager(), JerseyConfiguration.none(), monitorMock);
        jettyService.start();
    }

    @NotNull
    private Response executeRequest(String url) {

        try {
            var client = testOkHttpClient();
            var request = new Request.Builder().url(url).build();
            return client.newCall(request).execute();
        } catch (IOException e) {
            fail(e);
        }
        return null;
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
