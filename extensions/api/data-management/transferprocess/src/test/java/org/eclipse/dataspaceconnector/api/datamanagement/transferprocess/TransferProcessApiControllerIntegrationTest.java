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

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
class TransferProcessApiControllerIntegrationTest {

    public static final String PROCESS_ID = "processId";
    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(port),
                "web.http.data.path", "/api/v1/data",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    void getAllTransferProcesses(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID));

        baseRequest()
                .get("/transferprocess")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/data")
                .header("x-api-key", authKey)
                .when();
    }

    @Test
    void getTransferProcesses_withLimit(TransferProcessStore store) {
        store.create(createTransferProcess("processId_1"));
        store.create(createTransferProcess("processId_2"));

        baseRequest()
                .get("/transferprocess?offset=0&limit=15")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(2));
    }

    @Test
    void getTransferProcesses_withLimitAndOffset(TransferProcessStore store) {
        store.create(createTransferProcess("processId_1"));
        store.create(createTransferProcess("processId_2"));

        baseRequest()
                .get("/transferprocess?offset=1&limit=1")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getSingleTransferProcess(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID));
        baseRequest()
                .get("/transferprocess/" + PROCESS_ID)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is(PROCESS_ID));

    }

    @Test
    void getSingletransferProcess_notFound() {
        baseRequest()
                .get("/transferprocess/nonExistingId")
                .then()
                .statusCode(404);
    }

    @Test
    void getSingleTransferProcessState(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID, PROVISIONING.code()));

        var state = baseRequest()
                .get("/transferprocess/" + PROCESS_ID + "/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().asString();

        assertThat(state).isEqualTo("PROVISIONING");
    }

    @Test
    void getSingletransferProcessState_notFound() {
        baseRequest()
                .get("/transferprocess/nonExistingId/state")
                .then()
                .statusCode(404);
    }

    @Test
    void cancel(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID, IN_PROGRESS.code()));

        baseRequest()
                .contentType(JSON)
                .post("/transferprocess/" + PROCESS_ID + "/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    void cancel_notFound() {
        baseRequest()
                .contentType(JSON)
                .post("/transferprocess/nonExistingId/cancel")
                .then()
                .statusCode(404);
    }

    @Test
    void cancel_conflict(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID, COMPLETED.code()));
        baseRequest()
                .contentType(JSON)
                .post("/transferprocess/" + PROCESS_ID + "/cancel")
                .then()
                .statusCode(409);
    }

    @Test
    void deprovision(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID, COMPLETED.code()));

        baseRequest()
                .contentType(JSON)
                .post("/transferprocess/" + PROCESS_ID + "/deprovision")
                .then()
                .statusCode(204);
    }

    @Test
    void deprovision__notFound() {
        baseRequest()
                .contentType(JSON)
                .post("/transferprocess/nonExistingId/deprovision")
                .then()
                .statusCode(404);
    }

    @Test
    void deprovision__conflict(TransferProcessStore store) {
        store.create(createTransferProcess(PROCESS_ID, INITIAL.code()));
        baseRequest()
                .contentType(JSON)
                .post("/transferprocess/" + PROCESS_ID + "/deprovision")
                .then()
                .statusCode(409);
    }

    private TransferProcess createTransferProcess(String processId) {
        return createTransferProcessBuilder()
                .id(processId)
                .dataRequest(DataRequest.Builder.newInstance().destinationType("file").build())
                .build();
    }

    private TransferProcess createTransferProcess(String processId, int state) {
        return createTransferProcessBuilder()
                .id(processId)
                .state(state)
                .dataRequest(DataRequest.Builder.newInstance().destinationType("file").build())
                .build();
    }

    private TransferProcess.Builder createTransferProcessBuilder() {
        return TransferProcess.Builder.newInstance();
    }

}
