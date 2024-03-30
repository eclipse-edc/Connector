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

package org.eclipse.edc.connector.controlplane.api.transferprocess;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
public class TransferProcessControlApiControllerTest extends RestControllerTestBase {

    private final TransferProcessService transferProcessService = mock();

    @Test
    void complete() {
        var id = "tpId";
        when(transferProcessService.complete(id)).thenReturn(ServiceResult.success());
        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post(format("/%s/complete", id))
                .then()
                .log().ifError()
                .statusCode(204);
    }

    @Test
    void fail() {
        var id = "tpId";
        var body = """ 
                {
                    "errorMessage": "testError"
                }
                """;

        when(transferProcessService.terminate(any())).thenReturn(ServiceResult.success());
        baseRequest()
                .contentType(JSON)
                .body(body)
                .post(format("/%s/fail", id))
                .then()
                .log().ifError()
                .statusCode(204);

        var captor = ArgumentCaptor.forClass(TerminateTransferCommand.class);

        verify(transferProcessService).terminate(captor.capture());

        assertThat(captor.getValue().getReason()).isEqualTo("testError");
        assertThat(captor.getValue().getEntityId()).isEqualTo(id);
    }

    @Override
    protected Object controller() {
        return new TransferProcessControlApiController(transferProcessService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/transferprocess")
                .when();
    }
}
