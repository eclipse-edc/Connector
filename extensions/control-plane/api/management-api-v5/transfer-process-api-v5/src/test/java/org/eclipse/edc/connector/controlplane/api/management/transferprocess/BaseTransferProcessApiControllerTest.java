/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.transferprocess;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyList;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class BaseTransferProcessApiControllerTest extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final AuthorizationService authorizationService = mock();
    protected final TransferProcessService service = mock();
    protected final ParticipantContextService participantContextService = mock();
    private final String participantContextId = "test-participant-context-id";

    protected abstract String versionPath();

    private RequestSpecification baseRequest(String participantContextId) {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath() + "/participants/" + participantContextId)
                .when();
    }

    @BeforeEach
    void setup() {
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    @NotNull
    private TransferProcess.Builder createTransferProcess() {
        return TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString());
    }

    @Nested
    class Get {
        @Test
        void shouldReturnTransferProcess() {
            var transferProcess = createTransferProcess().id("id").build();
            var responseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(service.findById(any())).thenReturn(transferProcess);
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

            baseRequest(participantContextId)
                    .get("/transferprocesses/id")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is("id"))
                    .body("createdAt", is(1234));
            verify(service).findById("id");
            verify(transformerRegistry).transform(transferProcess, JsonObject.class);
        }

        @Test
        void shouldReturnNotFound_whenNotFound() {
            when(service.findById(any())).thenReturn(null);

            baseRequest(participantContextId)
                    .get("/transferprocesses/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
            verifyNoInteractions(transformerRegistry);
        }

        @Test
        void shouldReturnNotFound_whenTransformFails() {
            when(service.findById(any())).thenReturn(createTransferProcess().build());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));

            baseRequest(participantContextId)
                    .get("/transferprocesses/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
        }
    }

    @Nested
    class GetState {

        @Test
        void shouldReturnTheState() {
            when(service.getState(any())).thenReturn("INITIAL");
            var result = Json.createObjectBuilder().add("state", "INITIAL").build();
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(result));

            baseRequest(participantContextId)
                    .get("/transferprocesses/id/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("state", is("INITIAL"));
            verify(service).getState("id");
        }

        @Test
        void shouldReturnNotFound_whenTransferProcessIsNotFound() {
            when(service.getState(any())).thenReturn(null);

            baseRequest(participantContextId)
                    .get("/transferprocesses/id/state")
                    .then()
                    .statusCode(404);
            verify(service).getState("id");
        }
    }

    @Nested
    class Request {

        @Test
        void shouldReturnQueriedTransferProcesses() {
            var querySpec = QuerySpec.none();
            var expandedRequestBody = Json.createObjectBuilder().build();
            var transferProcess = createTransferProcess().id("id").build();
            var expandedResponseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(transferProcess)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedResponseBody));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1))
                    .body("[0].id", is("id"))
                    .body("[0].createdAt", is(1234));
            verify(transformerRegistry).transform(expandedRequestBody, QuerySpec.class);
            verify(service).search(argThat(s -> s.getOffset() == querySpec.getOffset() &&
                    s.getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().equals("participantContextId") &&
                            c.getOperator().equals("=") &&
                            c.getOperandRight().equals(participantContextId))));
            verify(transformerRegistry).transform(transferProcess, JsonObject.class);
        }

        @Test
        void shouldNotReturnError_whenEmptyBody() {
            var querySpec = QuerySpec.none();
            when(service.search(any())).thenReturn(ServiceResult.success(emptyList()));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/transferprocesses/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));

            verify(service).search(argThat(s -> s.getOffset() == querySpec.getOffset() &&
                    s.getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().equals("participantContextId") &&
                            c.getOperator().equals("=") &&
                            c.getOperandRight().equals(participantContextId))));

        }

        @Test
        void shouldReturn400_whenQuerySpecTransformFails() {
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses/request")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnBadRequest_whenServiceReturnsBadRequest() {
            var querySpec = QuerySpec.none();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.badRequest("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses/request")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
        }

        @Test
        void shouldFilterOutResults_whenTransformFails() {
            var querySpec = QuerySpec.none();
            var transferProcess = createTransferProcess().id("id").build();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(transferProcess)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));
        }
    }

    @Nested
    class Initiate {

        @Test
        void shouldReturnId() {
            var transferRequest = TransferRequest.Builder.newInstance().build();
            var transferProcess = createTransferProcess().id("id").build();
            var responseBody = Json.createObjectBuilder().add(ID, "transferProcessId").build();
            when(participantContextService.getParticipantContext(any()))
                    .thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build()));
            when(transformerRegistry.transform(any(), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
            when(service.initiateTransfer(isA(ParticipantContext.class), any())).thenReturn(ServiceResult.success(transferProcess));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("transferProcessId"));
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(TransferRequest.class));
            verify(service).initiateTransfer(any(), eq(transferRequest));
            verify(transformerRegistry).transform(isA(IdResponse.class), eq(JsonObject.class));
        }

        @Test
        void shouldReturnBadRequest_whenTransformationFails() {
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnConflict_whenItAlreadyExists() {
            var transferRequest = TransferRequest.Builder.newInstance().build();
            when(participantContextService.getParticipantContext(any()))
                    .thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build()));
            when(transformerRegistry.transform(any(), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
            when(service.initiateTransfer(any(), any())).thenReturn(ServiceResult.conflict("already exists"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/transferprocesses")
                    .then()
                    .statusCode(409)
                    .contentType(JSON);
        }
    }

    @Nested
    class Terminate {

        @Test
        void shouldTerminate() {
            var expanded = Json.createObjectBuilder().build();
            var terminateTransfer = new TerminateTransfer("anyReason");
            when(transformerRegistry.transform(any(), eq(TerminateTransfer.class))).thenReturn(Result.success(terminateTransfer));
            when(service.terminate(any())).thenReturn(ServiceResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/transferprocesses/id/terminate")
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(expanded, TerminateTransfer.class);
            verify(service).terminate(isA(TerminateTransferCommand.class));
        }

        @Test
        void shouldReturnBadRequest_whenTransformFail() {
            when(transformerRegistry.transform(any(), eq(TerminateTransfer.class))).thenReturn(Result.failure("failure"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/transferprocesses/id/terminate")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            var terminateTransfer = new TerminateTransfer("anyReason");
            when(transformerRegistry.transform(any(), eq(TerminateTransfer.class))).thenReturn(Result.success(terminateTransfer));
            when(service.terminate(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/transferprocesses/id/terminate")
                    .then()
                    .statusCode(409);
        }

    }

    @Nested
    class Suspend {

        @Test
        void shouldSuspend() {
            var expanded = Json.createObjectBuilder().build();
            var suspendTransfer = new SuspendTransfer("anyReason");
            when(transformerRegistry.transform(any(), eq(SuspendTransfer.class))).thenReturn(Result.success(suspendTransfer));
            when(service.suspend(any())).thenReturn(ServiceResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/transferprocesses/id/suspend")
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(expanded, SuspendTransfer.class);
            verify(service).suspend(isA(SuspendTransferCommand.class));
        }

        @Test
        void shouldReturnBadRequest_whenTransformFail() {
            when(transformerRegistry.transform(any(), eq(SuspendTransfer.class))).thenReturn(Result.failure("failure"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/transferprocesses/id/suspend")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            var suspendTransfer = new SuspendTransfer("anyReason");
            when(transformerRegistry.transform(any(), eq(SuspendTransfer.class))).thenReturn(Result.success(suspendTransfer));
            when(service.suspend(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/transferprocesses/id/suspend")
                    .then()
                    .statusCode(409);
        }

    }

    @Nested
    class Resume {

        @Test
        void shouldResumeTransfer() {
            when(service.resume(any())).thenReturn(ServiceResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/transferprocesses/id/resume")
                    .then()
                    .statusCode(204);
            verify(service).resume(isA(ResumeTransferCommand.class));
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            when(service.resume(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/transferprocesses/id/resume")
                    .then()
                    .statusCode(409);
        }

        @Test
        void shouldReturnNotFound_whenServiceReturnsNotFound() {
            when(service.resume(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/transferprocesses/id/resume")
                    .then()
                    .statusCode(404);
        }

    }

}
