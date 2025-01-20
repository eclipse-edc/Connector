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

package org.eclipse.edc.web.jersey.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ApiTest
@ExtendWith(RuntimePerMethodExtension.class)
public class ExceptionMappersIntegrationTest {

    private final int port = getFreePort();
    private final Runnable runnable = mock(Runnable.class);

    @BeforeEach
    void setUp(RuntimeExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(port),
                "web.http.path", "/api"
        ));
        extension.registerSystemExtension(ServiceExtension.class, new MyServiceExtension());
    }

    @Test
    void shouldListEdcApiExceptionMessageInResponseBody() {
        doThrow(new ObjectNotFoundException(String.class, "anId")).when(runnable).run();

        given()
                .port(port)
                .accept(JSON)
                .get("/api/test")
                .then()
                .statusCode(404)
                .contentType(JSON)
                .body("[0].message", containsString("not found"))
                .body("[0].message", containsString("anId"))
                .body("[0].type", is("ObjectNotFound"));
    }

    @Test
    void shouldReturn500ErrorOnJavaLangExceptions() {
        doThrow(new NullPointerException()).when(runnable).run();

        given()
                .port(port)
                .accept(JSON)
                .get("/api/test")
                .then()
                .statusCode(500);
    }

    private static class RequestPayload {
        @JsonProperty
        private String data;

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
        public void doAction(RequestPayload payload) {
        }

    }

    private class MyServiceExtension implements ServiceExtension {
        @Inject
        private WebService webService;

        @Override
        public void initialize(ServiceExtensionContext context) {
            webService.registerResource(new TestController());
        }
    }
}
