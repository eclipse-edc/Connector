/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api;

import io.restassured.http.ContentType;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.signaling.domain.DataFlowStatusMessage;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataPlaneTransferApiControllerTest extends RestControllerTestBase {

    private final TransferProcessService transferProcessService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final DataPlaneSelectorService dataPlaneSelectorService = mock();
    private final SignalingAuthorizationRegistry signalingAuthorizationRegistry = mock();

    @Nested
    class AuthorizationFilter {
        @Test
        void shouldReturnUnauthorized_whenTransferDoesNotExist() {
            when(transferProcessService.findById(any())).thenReturn(null);
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

            verify(transferProcessService).findById(transferId);
            verify(transferProcessService, never()).notifyPrepared(any());
        }

        @Test
        void shouldReturnUnauthorized_whenDataPlaneRelatedToTransferProcessDoesNotExist() {
            var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("unexistent").build();
            when(transferProcessService.findById(any())).thenReturn(transferProcess);
            when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.notFound("not found"));
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

            verify(dataPlaneSelectorService).findById("unexistent");
            verify(transferProcessService, never()).notifyPrepared(any());
        }

        @Test
        void shouldNotEnforceAuthorization_whenDataPlaneHasNoAuthorizationProfile() {
            var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("dataPlaneId").build();
            when(transferProcessService.findById(any())).thenReturn(transferProcess);
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://any").authorizationProfile(null).build();
            when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();

            when(typeTransformerRegistry.transform(any(), eq(DataFlowResponse.class))).thenReturn(Result.success(createDataFlowResponse()));
            when(transferProcessService.notifyPrepared(any())).thenReturn(ServiceResult.success());

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verifyNoInteractions(signalingAuthorizationRegistry);
        }

        @Test
        void shouldReturnInternalServerError_whenAuthorizationRegisteredOnDataplaneIsNotSupported() {
            var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("dataPlaneId").build();
            when(transferProcessService.findById(any())).thenReturn(transferProcess);
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                    .url("http://localhost/any")
                    .authorizationProfile(new AuthorizationProfile("unsupported", emptyMap())).build();
            when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(signalingAuthorizationRegistry.findByType(any())).thenReturn(null);
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(500);

            verify(signalingAuthorizationRegistry).findByType("unsupported");
        }

        @Test
        void shouldReturnUnauthorized_whenAuthorizationFails() {
            var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("dataPlaneId").build();
            when(transferProcessService.findById(any())).thenReturn(transferProcess);
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                    .url("http://localhost/any")
                    .authorizationProfile(new AuthorizationProfile("type", emptyMap())).build();
            when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            SignalingAuthorization authorization = mock();
            when(authorization.isAuthorized(any())).thenReturn(Result.failure("not authorized"));
            when(signalingAuthorizationRegistry.findByType(any())).thenReturn(authorization);
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

            verify(authorization).isAuthorized(any());
        }

        @Test
        void shouldExecuteLogic_whenAuthorizationSucceeds() {
            var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("dataPlaneId").build();
            when(transferProcessService.findById(any())).thenReturn(transferProcess);
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                    .url("http://localhost/any")
                    .authorizationProfile(new AuthorizationProfile("type", emptyMap())).build();
            when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            SignalingAuthorization authorization = mock();
            when(authorization.isAuthorized(any())).thenReturn(Result.success("dataPlaneId"));
            when(signalingAuthorizationRegistry.findByType(any())).thenReturn(authorization);
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();
            when(typeTransformerRegistry.transform(any(), eq(DataFlowResponse.class))).thenReturn(Result.success(createDataFlowResponse()));
            when(transferProcessService.notifyPrepared(any())).thenReturn(ServiceResult.success());

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(authorization).isAuthorized(any());
        }

        @Test
        void shouldReturnUnauthorized_whenAuthorizationSucceedsButDataPlaneIdIsNotTheOneExpected() {
            var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("dataPlaneId").build();
            when(transferProcessService.findById(any())).thenReturn(transferProcess);
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                    .url("http://localhost/any")
                    .authorizationProfile(new AuthorizationProfile("type", emptyMap())).build();
            when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            SignalingAuthorization authorization = mock();
            when(authorization.isAuthorized(any())).thenReturn(Result.success("differentDataPlaneId"));
            when(signalingAuthorizationRegistry.findByType(any())).thenReturn(authorization);
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

            verify(authorization).isAuthorized(any());
        }

    }

    @Nested
    class Prepared {

        @BeforeEach
        void setUp() {
            setupValidAuthorization();
        }

        @Test
        void shouldCallNotifyPrepared() {
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();
            var dataFlowResponse = createDataFlowResponse();

            when(typeTransformerRegistry.transform(any(), eq(DataFlowResponse.class))).thenReturn(Result.success(dataFlowResponse));
            when(transferProcessService.notifyPrepared(any())).thenReturn(ServiceResult.success());

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(typeTransformerRegistry).transform(any(DataFlowStatusMessage.class), eq(DataFlowResponse.class));
            verify(transferProcessService).notifyPrepared(argThat(c -> c != null &&
                    c.getEntityId().equals(transferId) &&
                    c.getDataAddress().equals(dataFlowResponse.getDataAddress())));
        }

        @Test
        void shouldReturnBadRequest_whenTransformationFails() {
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();
            when(typeTransformerRegistry.transform(any(), eq(DataFlowResponse.class))).thenReturn(Result.failure("error"));

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void shouldReturnConflict_whenServiceCallFails() {
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowStatusMessage.Builder.newInstance().build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("test").build()).build();
            when(typeTransformerRegistry.transform(any(), eq(DataFlowResponse.class))).thenReturn(Result.success(dataFlowResponse));
            when(transferProcessService.notifyPrepared(any())).thenReturn(ServiceResult.conflict("error"));

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/prepared", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }
    }

    @Nested
    class Completed {

        @BeforeEach
        void setUp() {
            setupValidAuthorization();
        }

        @Test
        void shouldCallComplete() {
            var transferId = UUID.randomUUID().toString();
            when(transferProcessService.complete(any())).thenReturn(ServiceResult.success());

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .post("/transfers/{transferId}/dataflow/completed", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(transferProcessService).complete(transferId);
        }

        @Test
        void shouldReturnError_whenServiceCallFails() {
            var transferId = UUID.randomUUID().toString();
            when(transferProcessService.complete(any())).thenReturn(ServiceResult.conflict("error"));

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .post("/transfers/{transferId}/dataflow/completed", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }
    }

    @Nested
    class Errored {
        @BeforeEach
        void setUp() {
            setupValidAuthorization();
        }

        @Test
        void shouldLogWarning() {
            var transferId = UUID.randomUUID().toString();
            when(transferProcessService.complete(any())).thenReturn(ServiceResult.success());
            var message = DataFlowStatusMessage.Builder.newInstance().error("data-plane error").build();

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .post("/transfers/{transferId}/dataflow/errored", transferId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(monitor).warning(contains("data-plane error"));
        }
    }

    @Override
    protected Object controller() {
        return new DataPlaneTransferApiController(transferProcessService, typeTransformerRegistry, monitor);
    }

    @Override
    protected Object additionalResource() {
        return new DataPlaneTransferAuthorizationFilter(signalingAuthorizationRegistry, transferProcessService, dataPlaneSelectorService);
    }

    private DataFlowResponse createDataFlowResponse() {
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        return DataFlowResponse.Builder.newInstance().dataAddress(dataAddress).build();
    }

    private void setupValidAuthorization() {
        var transferProcess = TransferProcess.Builder.newInstance().dataPlaneId("dataPlaneId").build();
        when(transferProcessService.findById(any())).thenReturn(transferProcess);
        var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                .url("http://localhost/any")
                .authorizationProfile(new AuthorizationProfile("type", emptyMap())).build();
        when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));
        SignalingAuthorization authorization = mock();
        when(authorization.isAuthorized(any())).thenReturn(Result.success("dataPlaneId"));
        when(signalingAuthorizationRegistry.findByType(any())).thenReturn(authorization);
    }
}
