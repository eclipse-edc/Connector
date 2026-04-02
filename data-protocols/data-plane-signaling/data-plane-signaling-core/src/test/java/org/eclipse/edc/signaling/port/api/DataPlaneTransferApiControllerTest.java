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
import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferApiControllerTest extends RestControllerTestBase {

    private final TransferProcessService transferProcessService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();

    @Nested
    class Prepared {
        @Test
        void shouldCallNotifyPrepared() {
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowResponseMessage.Builder.newInstance().build();
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataAddress(dataAddress).build();

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

            verify(typeTransformerRegistry).transform(any(DataFlowResponseMessage.class), eq(DataFlowResponse.class));
            verify(transferProcessService).notifyPrepared(argThat(c -> c != null &&
                    c.getEntityId().equals(transferId) &&
                    c.getDataAddress().equals(dataAddress)));
        }

        @Test
        void shouldReturnBadRequest_whenTransformationFails() {
            var transferId = UUID.randomUUID().toString();
            var message = DataFlowResponseMessage.Builder.newInstance().build();
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
            var message = DataFlowResponseMessage.Builder.newInstance().build();
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


    @Override
    protected Object controller() {
        return new DataPlaneTransferApiController(transferProcessService, typeTransformerRegistry);
    }
}
