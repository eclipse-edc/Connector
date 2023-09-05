/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.dataplane.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.OutputStreamDataSinkFactory;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.stop.Stop.stopQuietly;

@ApiTest
@ExtendWith(EdcExtension.class)
class DataPlaneApiIntegrationTest {

    private static final ObjectMapper MAPPER = new TypeManager().getMapper();

    private static final int PUBLIC_API_PORT = getFreePort();
    private static final int CONTROL_API_PORT = getFreePort();
    private static final int VALIDATION_API_PORT = getFreePort();
    private static final String VALIDATION_SEVER_URL = "http://localhost:" + VALIDATION_API_PORT;

    private static ClientAndServer tokenValidationServer;
    private DataPlaneManager dataPlaneManager;

    @BeforeAll
    public static void startServer() {
        tokenValidationServer = ClientAndServer.startClientAndServer(VALIDATION_API_PORT);
    }

    @AfterAll
    public static void stopServer() {
        stopQuietly(tokenValidationServer);
    }

    @BeforeEach
    void setUp(EdcExtension extension) {
        dataPlaneManager = mock(DataPlaneManager.class);
        extension.registerSystemExtension(ServiceExtension.class, new TestServiceExtension());
        extension.setConfiguration(Map.of(
                "web.http.public.port", String.valueOf(PUBLIC_API_PORT),
                "web.http.public.path", "/public",
                "web.http.control.port", String.valueOf(CONTROL_API_PORT),
                "web.http.control.path", "/control",
                "edc.dataplane.token.validation.endpoint", VALIDATION_SEVER_URL
        ));
    }

    @AfterEach
    public void tearDown() {
        tokenValidationServer.reset();
    }

    @Test
    void controlApi_should_callDataPlaneManager_if_requestIsValid() {
        var flowRequest = DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(testDestAddress())
                .destinationDataAddress(testDestAddress())
                .build();

        when(dataPlaneManager.validate(isA(DataFlowRequest.class))).thenReturn(Result.success(Boolean.TRUE));

        given().port(CONTROL_API_PORT)
                .when()
                .contentType(ContentType.JSON)
                .body(flowRequest)
                .post("/control/transfer")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        verify(dataPlaneManager).initiate(isA(DataFlowRequest.class));
    }

    @Test
    void controlApi_should_returnBadRequest_if_requestIsInValid() {
        var errorMsg = "test error message";
        var flowRequest = DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(testDestAddress())
                .destinationDataAddress(testDestAddress())
                .build();

        when(dataPlaneManager.validate(isA(DataFlowRequest.class))).thenReturn(Result.failure(errorMsg));

        given().port(CONTROL_API_PORT)
                .when()
                .contentType(ContentType.JSON)
                .body(flowRequest)
                .post("/control/transfer")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("errors", CoreMatchers.equalTo(List.of(errorMsg)));

        verify(dataPlaneManager, never()).initiate(any());
    }

    @Test
    void publicApi_should_returnBadRequest_if_missingAuthorizationHeader() {
        given().port(PUBLIC_API_PORT)
                .when()
                .post("/public/any")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("errors[0]", is("Missing bearer token"));
    }

    @Test
    void publicApi_shouldNotReturn302_whenUrlWithoutTrailingSlash() {
        given().port(PUBLIC_API_PORT)
                .when()
                .post("/public")
                .then()
                .statusCode(not(302));
    }

    @Test
    void publicApi_should_returnForbidden_if_tokenValidationFails() {
        var token = UUID.randomUUID().toString();

        var validationServerRequest = new HttpRequest().withHeader(AUTHORIZATION, token);
        tokenValidationServer.when(validationServerRequest, once()).respond(new HttpResponse().withStatusCode(400));

        given().port(PUBLIC_API_PORT)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                .body("errors.size()", is(1));

        tokenValidationServer.verify(validationServerRequest, VerificationTimes.once());
    }

    @Test
    void publicApi_should_returnBadRequest_if_requestValidationFails() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();
        tokenValidationServer.when(new HttpRequest().withHeader(AUTHORIZATION, token), once())
                .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(MAPPER.writeValueAsString(testDestAddress()))
                );
        when(dataPlaneManager.validate(any())).thenReturn(Result.failure(errorMsg));

        given()
                .port(PUBLIC_API_PORT)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("errors.size()", is(1));
    }

    @Test
    void publicApi_should_returnInternalServerError_if_transferFails() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();
        tokenValidationServer.when(new HttpRequest().withHeader(AUTHORIZATION, token), once())
                .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(MAPPER.writeValueAsString(testDestAddress()))
                );
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any()))
                .thenReturn(completedFuture(StreamResult.error(errorMsg)));

        given()
                .port(PUBLIC_API_PORT)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("errors[0]", is(errorMsg));
    }

    @Test
    void publicApi_should_returnInternalServerError_if_transferThrows() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();
        tokenValidationServer.when(new HttpRequest().withHeader(AUTHORIZATION, token), once())
                .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(MAPPER.writeValueAsString(testDestAddress()))
                );
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(failedFuture(new RuntimeException(errorMsg)));

        given()
                .port(PUBLIC_API_PORT)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("errors[0]", is("Unhandled exception occurred during data transfer: " + errorMsg));
    }

    @Test
    void publicApi_should_returnDataFromSource_if_transferSuccessful() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var address = testDestAddress();
        var requestCaptor = ArgumentCaptor.forClass(DataFlowRequest.class);

        tokenValidationServer.when(new HttpRequest().withHeader(AUTHORIZATION, token), once())
                .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(MAPPER.writeValueAsString(address))
                );
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any()))
                .thenReturn(completedFuture(StreamResult.success()));

        given()
                .port(PUBLIC_API_PORT)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any?foo=bar")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        verify(dataPlaneManager).validate(requestCaptor.capture());
        verify(dataPlaneManager).transfer(ArgumentCaptor.forClass(DataSink.class).capture(), requestCaptor.capture());
        var capturedRequests = requestCaptor.getAllValues();
        assertThat(capturedRequests)
                .hasSize(2)
                .allSatisfy(request -> {
                    assertThat(request.getDestinationDataAddress().getType()).isEqualTo(OutputStreamDataSinkFactory.TYPE);
                    assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
                    assertThat(request.getProperties()).containsEntry("method", "POST").containsEntry("pathSegments", "any").containsEntry("queryParams", "foo=bar");
                });
    }

    private DataAddress testDestAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }

    @Provides(DataPlaneManager.class)
    private class TestServiceExtension implements ServiceExtension {
        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(DataPlaneManager.class, dataPlaneManager);
        }
    }
}
