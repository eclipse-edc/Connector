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

package org.eclipse.edc.connector.dataplane.api.controller;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DataPlaneControlApiControllerTest extends RestControllerTestBase {

    private final DataPlaneManager manager = mock();

    @Test
    void should_callDataPlaneManager_if_requestIsValid() {
        var flowRequest = DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(testDestAddress())
                .destinationDataAddress(testDestAddress())
                .build();

        when(manager.validate(isA(DataFlowStartMessage.class))).thenReturn(Result.success(Boolean.TRUE));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(flowRequest)
                .post("/transfer")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        verify(manager).initiate(isA(DataFlowStartMessage.class));
    }

    @Test
    void should_returnBadRequest_if_requestIsInValid() {
        var errorMsg = "test error message";
        var flowRequest = DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(testDestAddress())
                .destinationDataAddress(testDestAddress())
                .build();

        when(manager.validate(isA(DataFlowStartMessage.class))).thenReturn(Result.failure(errorMsg));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(flowRequest)
                .post("/transfer")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("errors", CoreMatchers.equalTo(List.of(errorMsg)));

        verify(manager, never()).initiate(any());
    }

    @Test
    void delete_shouldReturnOk_whenTerminationSucceeds() {
        when(manager.terminate(any())).thenReturn(StatusResult.success());

        baseRequest()
                .delete("/transfer/transferId")
                .then()
                .statusCode(204);

        verify(manager).terminate("transferId");
    }

    @Test
    void delete_shouldReturnError_whenTerminationFails() {
        when(manager.terminate(any())).thenReturn(StatusResult.failure(FATAL_ERROR));

        baseRequest()
                .delete("/transfer/transferId")
                .then()
                .statusCode(400);

        verify(manager).terminate("transferId");
    }

    @Override
    protected Object controller() {
        return new DataPlaneControlApiController(manager);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .header(HttpHeaders.AUTHORIZATION, "auth")
                .when();
    }

    private DataAddress testDestAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }
}
