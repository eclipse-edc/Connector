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

import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.resolver.DataAddressResolver;
import org.eclipse.edc.connector.dataplane.util.sink.OutputStreamDataSinkFactory;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DataPlanePublicApiControllerIntegrationTest extends RestControllerTestBase {

    private final PipelineService pipelineService = mock();
    private final DataAddressResolver dataAddressResolver = mock();

    @Test
    void should_returnBadRequest_if_missingAuthorizationHeader() {
        baseRequest()
                .post("/any")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("errors[0]", is("Missing bearer token"));
    }

    @Test
    void shouldNotReturn302_whenUrlWithoutTrailingSlash() {
        baseRequest()
                .post("")
                .then()
                .statusCode(not(302));
    }

    @Test
    void should_returnForbidden_if_tokenValidationFails() {
        var token = UUID.randomUUID().toString();
        when(dataAddressResolver.resolve(any())).thenReturn(Result.failure("token is not value"));

        baseRequest()
                .header(AUTHORIZATION, token)
                .post("/any")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                .body("errors.size()", is(1));

        verify(dataAddressResolver).resolve(token);
    }

    @Test
    void should_returnBadRequest_if_requestValidationFails() {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();
        when(dataAddressResolver.resolve(any())).thenReturn(Result.success(testDestAddress()));
        when(pipelineService.validate(any())).thenReturn(Result.failure(errorMsg));

        baseRequest()
                .header(AUTHORIZATION, token)
                .when()
                .post("/any")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("errors.size()", is(1));
    }

    @Test
    void should_returnInternalServerError_if_transferFails() {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();
        when(dataAddressResolver.resolve(any())).thenReturn(Result.success(testDestAddress()));
        when(pipelineService.validate(any())).thenReturn(Result.success(true));
        when(pipelineService.transfer(any()))
                .thenReturn(completedFuture(StreamResult.error(errorMsg)));

        baseRequest()
                .header(AUTHORIZATION, token)
                .when()
                .post("/any")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("errors[0]", is(errorMsg));
    }

    @Test
    void should_returnInternalServerError_if_transferThrows() {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();
        when(dataAddressResolver.resolve(any())).thenReturn(Result.success(testDestAddress()));
        when(pipelineService.validate(any())).thenReturn(Result.success(true));
        when(pipelineService.transfer(any(DataFlowRequest.class)))
                .thenReturn(failedFuture(new RuntimeException(errorMsg)));

        baseRequest()
                .header(AUTHORIZATION, token)
                .when()
                .post("/any")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("errors[0]", is("Unhandled exception occurred during data transfer: " + errorMsg));
    }

    @Test
    void should_returnDataFromSource_if_transferSuccessful() {
        var token = UUID.randomUUID().toString();
        var address = testDestAddress();
        var requestCaptor = ArgumentCaptor.forClass(DataFlowRequest.class);

        when(dataAddressResolver.resolve(any())).thenReturn(Result.success(testDestAddress()));
        when(pipelineService.validate(any())).thenReturn(Result.success(true));
        when(pipelineService.transfer(any()))
                .thenReturn(completedFuture(StreamResult.success()));

        baseRequest()
                .header(AUTHORIZATION, token)
                .when()
                .post("/any?foo=bar")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        verify(pipelineService).validate(requestCaptor.capture());
        verify(pipelineService).transfer(requestCaptor.capture());
        var capturedRequests = requestCaptor.getAllValues();
        assertThat(capturedRequests)
                .hasSize(2)
                .allSatisfy(request -> {
                    assertThat(request.getDestinationDataAddress().getType()).isEqualTo(OutputStreamDataSinkFactory.TYPE);
                    assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
                    assertThat(request.getProperties()).containsEntry("method", "POST").containsEntry("pathSegments", "any").containsEntry("queryParams", "foo=bar");
                });
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }

    private DataAddress testDestAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }

    @Override
    protected Object controller() {
        return new DataPlanePublicApiController(pipelineService, dataAddressResolver);
    }

}
