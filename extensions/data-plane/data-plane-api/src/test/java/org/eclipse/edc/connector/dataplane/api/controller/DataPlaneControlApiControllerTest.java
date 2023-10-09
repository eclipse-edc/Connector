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

import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DataPlaneControlApiControllerTest extends RestControllerTestBase {

    private final DataPlaneManager manager = mock();

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
}
