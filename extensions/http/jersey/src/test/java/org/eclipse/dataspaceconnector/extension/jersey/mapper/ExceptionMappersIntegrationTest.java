/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey.mapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyMap;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(EdcExtension.class)
public class ExceptionMappersIntegrationTest {

    private final int port = getFreePort();
    private final Runnable runnable = mock(Runnable.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.test.port", String.valueOf(port),
                "web.http.test.path", "/",
                "edc.web.rest.error.response.verbose", Boolean.TRUE.toString()
        ));
        extension.registerSystemExtension(ServiceExtension.class, new MyServiceExtension());
    }

    @Test
    void shouldListEdcApiExceptionMessageInResponseBody() {
        doThrow(new ObjectNotFoundException(String.class, "anId")).when(runnable).run();

        given()
                .port(port)
                .accept(JSON)
                .get("/test")
                .then()
                .statusCode(404)
                .contentType(JSON)
                .body("[0].message", containsString("not found"))
                .body("[0].message", containsString("anId"));
    }

    @Test
    void shouldListJakartaValidationMessageInResponseBody() {
        given()
                .port(port)
                .accept(JSON)
                .contentType(JSON)
                .body(emptyMap())
                .post("/test")
                .then()
                .statusCode(400)
                .body("[0].message", CoreMatchers.is("must not be null"));
    }

    @Path("/test")
    public class TestController {

        @GET
        @Produces("application/json")
        public Map<String, String> get() {
            runnable.run();
            return Map.of("value", "result that nobody will see");
        }

        @POST
        @Consumes("application/json")
        public void doAction(@Valid RequestPayload payload) {
        }

    }

    private static class RequestPayload {
        @NotNull
        private String data;
    }

    private class MyServiceExtension implements ServiceExtension {
        @Inject
        private WebService webService;

        @Override
        public void initialize(ServiceExtensionContext context) {
            webService.registerResource("test", new TestController());
        }
    }
}
