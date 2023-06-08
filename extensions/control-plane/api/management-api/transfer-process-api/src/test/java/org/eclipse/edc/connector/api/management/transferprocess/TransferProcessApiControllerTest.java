/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.transferprocess;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TerminateTransferDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class TransferProcessApiControllerTest extends RestControllerTestBase {

    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private final TransferProcessService service = mock(TransferProcessService.class);

    @Test
    void get_shouldReturnTransferProcess() {
        var transferProcess = createTransferProcess().id("id").build();
        var dto = TransferProcessDto.Builder.newInstance().id("id").createdAt(1234).build();
        var responseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
        when(transformerRegistry.transform(any(), eq(TransferProcessDto.class))).thenReturn(Result.success(dto));
        when(service.findById(any())).thenReturn(transferProcess);
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

        given()
                .port(port)
                .get("/v2/transferprocesses/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("id"))
                .body("createdAt", is(1234));
        verify(service).findById("id");
        verify(transformerRegistry).transform(transferProcess, TransferProcessDto.class);
        verify(transformerRegistry).transform(dto, JsonObject.class);
    }

    @Test
    void get_shouldReturnNotFound_whenNotFound() {
        when(service.findById(any())).thenReturn(null);

        given()
                .port(port)
                .get("/v2/transferprocesses/id")
                .then()
                .statusCode(404)
                .contentType(JSON);
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void get_shouldReturnNotFound_whenTransformFails() {
        when(service.findById(any())).thenReturn(createTransferProcess().build());
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));

        given()
                .port(port)
                .get("/v2/transferprocesses/id")
                .then()
                .statusCode(404)
                .contentType(JSON);
    }

    @Test
    void getState_shouldReturnTheState() {
        when(service.getState(any())).thenReturn("INITIAL");
        var result = Json.createObjectBuilder().add("state", "INITIAL").build();
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(result));

        given()
                .port(port)
                .get("/v2/transferprocesses/id/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("state", is("INITIAL"));
        verify(service).getState("id");
    }

    @Test
    void getState_shouldReturnNotFound_whenTransferProcessIsNotFound() {
        when(service.getState(any())).thenReturn(null);

        given()
                .port(port)
                .get("/v2/transferprocesses/id/state")
                .then()
                .statusCode(404);
        verify(service).getState("id");
    }

    @Test
    void query_shouldReturnQueriedTransferProcesses() {
        var querySpec = QuerySpec.none();
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();
        var expandedRequestBody = Json.createObjectBuilder().build();
        var transferProcess = createTransferProcess().id("id").build();
        var dto = TransferProcessDto.Builder.newInstance().id("id").createdAt(1234).build();
        var expandedResponseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
        var responseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
        when(transformerRegistry.transform(any(), eq(QuerySpecDto.class))).thenReturn(Result.success(querySpecDto));
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(transferProcess)));
        when(transformerRegistry.transform(any(), eq(TransferProcessDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedResponseBody));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1))
                .body("[0].id", is("id"))
                .body("[0].createdAt", is(1234));
        verify(transformerRegistry).transform(expandedRequestBody, QuerySpecDto.class);
        verify(transformerRegistry).transform(querySpecDto, QuerySpec.class);
        verify(service).query(querySpec);
        verify(transformerRegistry).transform(transferProcess, TransferProcessDto.class);
        verify(transformerRegistry).transform(dto, JsonObject.class);
    }

    @Test
    void query_shouldNotReturnError_whenEmptyBody() {
        var querySpec = QuerySpec.none();
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.empty()));

        given()
                .port(port)
                .contentType(JSON)
                .post("/v2/transferprocesses/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));

        verify(service).query(querySpec);
    }

    @Test
    void query_shouldReturn400_whenQuerySpecTransformFails() {
        var expandedRequestBody = Json.createObjectBuilder().build();
        when(transformerRegistry.transform(any(), eq(QuerySpecDto.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses/request")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void query_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
        var querySpec = QuerySpec.none();
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();
        var expandedRequestBody = Json.createObjectBuilder().build();
        when(transformerRegistry.transform(any(), eq(QuerySpecDto.class))).thenReturn(Result.success(querySpecDto));
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
        when(service.query(any())).thenReturn(ServiceResult.badRequest("error"));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses/request")
                .then()
                .statusCode(400)
                .contentType(JSON);
    }

    @Test
    void query_shouldFilterOutResults_whenTransformFails() {
        var querySpec = QuerySpec.none();
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();
        var expandedRequestBody = Json.createObjectBuilder().build();
        var transferProcess = createTransferProcess().id("id").build();
        when(transformerRegistry.transform(any(), eq(QuerySpecDto.class))).thenReturn(Result.success(querySpecDto));
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(transferProcess)));
        when(transformerRegistry.transform(any(), eq(TransferProcessDto.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));
    }

    @Test
    void initiate_shouldReturnId() {
        var dto = TransferRequestDto.Builder.newInstance().build();
        var transferRequest = TransferRequest.Builder.newInstance().dataRequest(createDataRequest()).build();
        var transferProcess = createTransferProcess().id("id").build();
        var responseBody = Json.createObjectBuilder().add(ID, "transferProcessId").build();
        when(transformerRegistry.transform(any(), eq(TransferRequestDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(any(), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
        when(service.initiateTransfer(any())).thenReturn(ServiceResult.success(transferProcess));
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(ID, is("transferProcessId"));
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(TransferRequestDto.class));
        verify(transformerRegistry).transform(dto, TransferRequest.class);
        verify(service).initiateTransfer(transferRequest);
        verify(transformerRegistry).transform(isA(IdResponseDto.class), eq(JsonObject.class));
    }

    @Test
    void initiate_shouldReturnBadRequest_whenTransformationFails() {
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses")
                .then()
                .statusCode(400)
                .contentType(JSON);
        verifyNoInteractions(service);
    }

    @Test
    void initiate_shouldReturnConflict_whenItAlreadyExists() {
        var dto = TransferRequestDto.Builder.newInstance().build();
        var transferRequest = TransferRequest.Builder.newInstance().dataRequest(createDataRequest()).build();
        when(transformerRegistry.transform(any(), eq(TransferRequestDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(any(), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
        when(service.initiateTransfer(any())).thenReturn(ServiceResult.conflict("already exists"));
        var requestBody = Json.createObjectBuilder().build();

        given()
                .port(port)
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/transferprocesses")
                .then()
                .statusCode(409)
                .contentType(JSON);
    }

    @Test
    void deprovision() {
        var transferProcess = createTransferProcess().build();
        when(service.deprovision(any())).thenReturn(ServiceResult.success(transferProcess));

        given()
                .port(port)
                .contentType(JSON)
                .post("/v2/transferprocesses/id/deprovision")
                .then()
                .statusCode(204);
        verify(service).deprovision("id");
    }

    @Test
    void deprovision_conflict() {
        when(service.deprovision(any())).thenReturn(ServiceResult.conflict("conflict"));

        given()
                .port(port)
                .contentType(JSON)
                .post("/v2/transferprocesses/id/deprovision")
                .then()
                .statusCode(409);
    }

    @Test
    void deprovision_NotFound() {
        when(service.deprovision(any())).thenReturn(ServiceResult.notFound("not found"));

        given()
                .port(port)
                .contentType(JSON)
                .post("/v2/transferprocesses/id/deprovision")
                .then()
                .statusCode(404);
    }

    @Test
    void terminate() {
        var transferProcess = createTransferProcess().build();
        var expanded = Json.createObjectBuilder().build();
        var dto = TerminateTransferDto.Builder.newInstance().reason("anyReason").build();
        when(transformerRegistry.transform(any(), eq(TerminateTransferDto.class))).thenReturn(Result.success(dto));
        when(service.terminate(any(), any())).thenReturn(ServiceResult.success(transferProcess));

        given()
                .port(port)
                .contentType(JSON)
                .body(Json.createObjectBuilder().build())
                .post("/v2/transferprocesses/id/terminate")
                .then()
                .statusCode(204);
        verify(transformerRegistry).transform(expanded, TerminateTransferDto.class);
        verify(service).terminate("id", "anyReason");
    }

    @Test
    void terminate_shouldReturnBadRequest_whenTransformFail() {
        when(transformerRegistry.transform(any(), eq(TerminateTransferDto.class))).thenReturn(Result.failure("failure"));

        given()
                .port(port)
                .contentType(JSON)
                .body(Json.createObjectBuilder().build())
                .post("/v2/transferprocesses/id/terminate")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void terminate_shouldReturnConflict_whenServiceReturnsConflict() {
        var expanded = Json.createObjectBuilder().build();
        var dto = TerminateTransferDto.Builder.newInstance().reason("anyReason").build();
        when(transformerRegistry.transform(any(), eq(TerminateTransferDto.class))).thenReturn(Result.success(dto));
        when(service.terminate(any(), any())).thenReturn(ServiceResult.conflict("conflict"));

        given()
                .port(port)
                .contentType(JSON)
                .post("/v2/transferprocesses/id/terminate")
                .then()
                .statusCode(409);
    }

    @Override
    protected Object controller() {
        return new TransferProcessApiController(monitor, service, transformerRegistry);
    }

    @NotNull
    private TransferProcess.Builder createTransferProcess() {
        return TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString());
    }

    private DataRequest createDataRequest() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("type")
                        .build())
                .protocol("dataspace-protocol-http")
                .processId(UUID.randomUUID().toString())
                .build();
    }
}
