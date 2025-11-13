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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
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
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyList;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer.SUSPEND_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer.TERMINATE_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class BaseTransferProcessApiControllerTest extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final TransferProcessService service = mock();
    protected final JsonObjectValidatorRegistry validatorRegistry = mock();
    protected final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(new ParticipantContext("participantId"));

    protected abstract RequestSpecification baseRequest();

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

            baseRequest()
                    .get("/id")
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

            baseRequest()
                    .get("/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
            verifyNoInteractions(transformerRegistry);
        }

        @Test
        void shouldReturnNotFound_whenTransformFails() {
            when(service.findById(any())).thenReturn(createTransferProcess().build());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));

            baseRequest()
                    .get("/id")
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

            baseRequest()
                    .get("/id/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("state", is("INITIAL"));
            verify(service).getState("id");
        }

        @Test
        void shouldReturnNotFound_whenTransferProcessIsNotFound() {
            when(service.getState(any())).thenReturn(null);

            baseRequest()
                    .get("/id/state")
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
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(transferProcess)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedResponseBody));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1))
                    .body("[0].id", is("id"))
                    .body("[0].createdAt", is(1234));
            verify(transformerRegistry).transform(expandedRequestBody, QuerySpec.class);
            verify(service).search(querySpec);
            verify(transformerRegistry).transform(transferProcess, JsonObject.class);
        }

        @Test
        void shouldNotReturnError_whenEmptyBody() {
            var querySpec = QuerySpec.none();
            when(service.search(any())).thenReturn(ServiceResult.success(emptyList()));

            baseRequest()
                    .contentType(JSON)
                    .post("/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));

            verify(service).search(querySpec);
            verifyNoInteractions(validatorRegistry, transformerRegistry);
        }

        @Test
        void shouldReturnBadRequest_whenValidationFails() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/request")
                    .then()
                    .statusCode(400);

            verify(validatorRegistry).validate(eq(EDC_QUERY_SPEC_TYPE), any());
            verifyNoInteractions(service, transformerRegistry);
        }

        @Test
        void shouldReturn400_whenQuerySpecTransformFails() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/request")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnBadRequest_whenServiceReturnsBadRequest() {
            var querySpec = QuerySpec.none();
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.badRequest("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/request")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
        }

        @Test
        void shouldFilterOutResults_whenTransformFails() {
            var querySpec = QuerySpec.none();
            var transferProcess = createTransferProcess().id("id").build();
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(transferProcess)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/request")
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
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
            when(service.initiateTransfer(any(), any())).thenReturn(ServiceResult.success(transferProcess));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("transferProcessId"));
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(TransferRequest.class));
            verify(service).initiateTransfer(any(), eq(transferRequest));
            verify(transformerRegistry).transform(isA(IdResponse.class), eq(JsonObject.class));
        }

        @Test
        void shouldReturnBadRequest_whenValidationFails() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);

            verify(validatorRegistry).validate(eq(TRANSFER_REQUEST_TYPE), any());
            verifyNoInteractions(service, transformerRegistry);
        }

        @Test
        void shouldReturnBadRequest_whenTransformationFails() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnConflict_whenItAlreadyExists() {
            var transferRequest = TransferRequest.Builder.newInstance().build();
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
            when(service.initiateTransfer(any(), any())).thenReturn(ServiceResult.conflict("already exists"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("")
                    .then()
                    .statusCode(409)
                    .contentType(JSON);
        }
    }

    @Nested
    class Deprovision {

        @Test
        void shouldDeprovision() {
            when(service.deprovision(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .contentType(JSON)
                    .post("/id/deprovision")
                    .then()
                    .statusCode(204);
            verify(service).deprovision("id");
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            when(service.deprovision(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest()
                    .contentType(JSON)
                    .post("/id/deprovision")
                    .then()
                    .statusCode(409);
        }

        @Test
        void shouldReturnNotFound_whenServiceReturnsNotFound() {
            when(service.deprovision(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .contentType(JSON)
                    .post("/id/deprovision")
                    .then()
                    .statusCode(404);
        }

    }

    @Nested
    class Terminate {

        @Test
        void shouldTerminate() {
            var expanded = Json.createObjectBuilder().build();
            var terminateTransfer = new TerminateTransfer("anyReason");
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TerminateTransfer.class))).thenReturn(Result.success(terminateTransfer));
            when(service.terminate(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/id/terminate")
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(expanded, TerminateTransfer.class);
            verify(service).terminate(isA(TerminateTransferCommand.class));
        }

        @Test
        void shouldReturnBadRequest_whenValidationFail() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));

            baseRequest()
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/id/terminate")
                    .then()
                    .statusCode(400);

            verify(validatorRegistry).validate(eq(TERMINATE_TRANSFER_TYPE), any());
            verifyNoInteractions(service, transformerRegistry);
        }

        @Test
        void shouldReturnBadRequest_whenTransformFail() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TerminateTransfer.class))).thenReturn(Result.failure("failure"));

            baseRequest()
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/id/terminate")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            var terminateTransfer = new TerminateTransfer("anyReason");
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TerminateTransfer.class))).thenReturn(Result.success(terminateTransfer));
            when(service.terminate(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest()
                    .contentType(JSON)
                    .post("/id/terminate")
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
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(SuspendTransfer.class))).thenReturn(Result.success(suspendTransfer));
            when(service.suspend(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/id/suspend")
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(expanded, SuspendTransfer.class);
            verify(service).suspend(isA(SuspendTransferCommand.class));
        }

        @Test
        void shouldReturnBadRequest_whenValidationFail() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));

            baseRequest()
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/id/suspend")
                    .then()
                    .statusCode(400);

            verify(validatorRegistry).validate(eq(SUSPEND_TRANSFER_TYPE), any());
            verifyNoInteractions(service, transformerRegistry);
        }

        @Test
        void shouldReturnBadRequest_whenTransformFail() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(SuspendTransfer.class))).thenReturn(Result.failure("failure"));

            baseRequest()
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/id/suspend")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            var suspendTransfer = new SuspendTransfer("anyReason");
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(SuspendTransfer.class))).thenReturn(Result.success(suspendTransfer));
            when(service.suspend(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest()
                    .contentType(JSON)
                    .post("/id/suspend")
                    .then()
                    .statusCode(409);
        }

    }

    @Nested
    class Resume {

        @Test
        void shouldResumeTransfer() {
            when(service.resume(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .contentType(JSON)
                    .post("/id/resume")
                    .then()
                    .statusCode(204);
            verify(service).resume(isA(ResumeTransferCommand.class));
        }

        @Test
        void shouldReturnConflict_whenServiceReturnsConflict() {
            when(service.resume(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest()
                    .contentType(JSON)
                    .post("/id/resume")
                    .then()
                    .statusCode(409);
        }

        @Test
        void shouldReturnNotFound_whenServiceReturnsNotFound() {
            when(service.resume(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .contentType(JSON)
                    .post("/id/resume")
                    .then()
                    .statusCode(404);
        }

    }

}
