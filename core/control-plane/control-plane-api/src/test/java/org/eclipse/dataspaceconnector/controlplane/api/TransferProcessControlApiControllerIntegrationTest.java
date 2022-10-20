/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.controlplane.api;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.controlplane.api.transferprocess.model.TransferProcessFailStateDto;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ERROR;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
class TransferProcessControlApiControllerIntegrationTest {

    private final int port = getFreePort();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.controlplane.port", String.valueOf(port),
                "web.http.controlplane.path", ControlPlaneApiExtension.DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH
        ));
    }

    @Test
    void callTransferProcessHookWithComplete(TransferProcessStore store) {

        store.create(createTransferProcess());


        baseRequest()
                .contentType("application/json")
                .post("/transferprocess/{processId}/complete", "tp-id")
                .then()
                .body(is(""))
                .statusCode(is(204));


        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull()
                    .extracting(StatefulEntity::getState).isEqualTo(COMPLETED.code());
        });
    }

    @Test
    void callTransferProcessHookWithError(TransferProcessStore store) {

        store.create(createTransferProcess());

        var rq = TransferProcessFailStateDto.Builder.newInstance()
                .errorMessage("error")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/transferprocess/{processId}/fail", "tp-id")
                .then()
                .body(is(""))
                .statusCode(is(204));


        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull().satisfies((process) -> {
                assertThat(process.getState()).isEqualTo(ERROR.code());
                assertThat(process.getErrorDetail()).isEqualTo("error");
            });
        });
    }

    @Test
    void callTransferProcessHookWithErrorFailWithNoErrorMessageBody(TransferProcessStore store) {
        baseRequest()
                .body("{}")
                .contentType("application/json")
                .post("/transferprocess/{processId}/fail", "tp-id")
                .then()
                .statusCode(is(400));

    }

    @Test
    void callTransferProcessHookWithErrorFailWithNoBody(TransferProcessStore store) {
        baseRequest()
                .contentType("application/json")
                .post("/transferprocess/{processId}/fail", "tp-id")
                .then()
                .statusCode(is(400));

    }

    private TransferProcess createTransferProcess() {
        return TransferProcess.Builder.newInstance()
                .id("tp-id")
                .state(TransferProcessStates.IN_PROGRESS.code())
                .type(TransferProcess.Type.PROVIDER)
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType("file")
                        .build())
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath(ControlPlaneApiExtension.DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH)
                .when();
    }


}
