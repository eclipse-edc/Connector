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

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
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

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.stop.Stop.stopQuietly;

@ExtendWith(EdcExtension.class)
class DataPlaneApiIntegrationTest {

    private static final Faker FAKER = new Faker();
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
                .id(FAKER.internet().uuid())
                .processId(FAKER.internet().uuid())
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

        verify(dataPlaneManager).initiateTransfer(isA(DataFlowRequest.class));
    }

    @Test
    void controlApi_should_returnBadRequest_if_requestIsInValid() {
        var errorMsg = FAKER.lorem().word();
        var flowRequest = DataFlowRequest.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .processId(FAKER.internet().uuid())
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

        verify(dataPlaneManager, never()).initiateTransfer(any());
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
    void publicApi_should_returnForbidden_if_tokenValidationFails() {
        var token = FAKER.internet().uuid();

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
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
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
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        tokenValidationServer.when(new HttpRequest().withHeader(AUTHORIZATION, token), once())
                .respond(new HttpResponse()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(MAPPER.writeValueAsString(testDestAddress()))
                );
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any()))
                .thenReturn(completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg)));

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
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
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
        var token = FAKER.internet().uuid();
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
                .thenReturn(completedFuture(StatusResult.success()));

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
