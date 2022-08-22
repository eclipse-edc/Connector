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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
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
                "web.http.path", "/api",
                "web.http.test.port", String.valueOf(port),
                "web.http.test.path", "/"
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
                .body("[0].message", containsString("anId"))
                .body("[0].type", is("ObjectNotFound"));
    }

    @Test
    void shouldListJakartaValidationMessageInResponseBody() {
        given()
                .port(port)
                .accept(JSON)
                .contentType(JSON)
                .body(Map.of("data", "", "number", "-1"))
                .post("/test")
                .then()
                .statusCode(400)
                .body("size()", is(2))
                .body("", hasItems(
                        hasEntry("message", "must be greater than 0"),
                        hasEntry("type", "{jakarta.validation.constraints.Positive.message}"),
                        hasEntry(is("path"), endsWith(".number")),
                        hasEntry("invalidValue", "-1")
                ))
                .body("", hasItems(
                        hasEntry("message", "must not be blank"),
                        hasEntry("type", "{jakarta.validation.constraints.NotBlank.message}"),
                        hasEntry(is("path"), endsWith(".data")),
                        hasEntry("invalidValue", "")
                ));
    }

    @Test
    void shouldReturn500ErrorOnJavaLangExceptions() {
        doThrow(new NullPointerException()).when(runnable).run();

        given()
                .port(port)
                .accept(JSON)
                .get("/test")
                .then()
                .statusCode(500);
    }

    private static class RequestPayload {
        @NotBlank
        @JsonProperty
        private String data;

        @Positive
        @JsonProperty
        private long number;
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

    private class MyServiceExtension implements ServiceExtension {
        @Inject
        private WebService webService;

        @Override
        public void initialize(ServiceExtensionContext context) {
            webService.registerResource("test", new TestController());
        }
    }
}
