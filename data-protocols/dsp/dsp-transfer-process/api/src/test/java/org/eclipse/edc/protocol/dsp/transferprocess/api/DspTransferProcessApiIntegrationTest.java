/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.jsonld.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.*;

@ExtendWith(EdcExtension.class)
public class DspTransferProcessApiIntegrationTest {
    private final int port = getFreePort();

    private final String authKey = "123456";

    private JsonLdTransformerRegistryImpl registry;

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(getFreePort()),
                "web.http.management.path", "/api/v1/management",
                "web.http.protocol.port", String.valueOf(port),
                "web.http.protocol.path", "/api/v1/dsp",
                "edc.api.auth.key", authKey,
                "edc.ids.id", "testID"
        ));


    }

    @Test
    public void getTransferProcess(TransferProcessStore transferProcessStore) {
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("testId")
                .build();

        transferProcessStore.save(transferProcess);


        baseRequest()
                .get("/transfers/testId")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void initiateTransferProcess() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createTransferRequestMessageJsonBody())
                .post("/transfers/request")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void consumerTransferProcessStart() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createTransferStartMessageJsonBody())
                .post("/transfers/0/start")
                .then()
                .statusCode(204)
                .contentType("");
    }

    @Test
    public void consumerTransferProcessCompletion() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createTransferCompletionMessageJsonBody())
                .post("/transfers/0/completion")
                .then()
                .statusCode(204)
                .contentType("");
    }

    @Test
    public void consumerTransferProcessTermination() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createTransferTerminationMessageJsonBody())
                .post("/transfers/0/termination")
                .then()
                .statusCode(204)
                .contentType("");
    }

    @Test
    public void consumerTransferProcessSuspension() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createTransferSuspensionMessageJsonBody())
                .post("/transfers/0/suspension")
                .then()
                .statusCode(204)
                .contentType("");
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/dsp")
                .when();
    }

    private JsonObject createTransferRequestMessageJsonBody() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_TRANSFERPROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACTAGREEMENT_TYPE, "testType")
                .add("dataAddress", "{}")
                .build();
    }

    private JsonObject createTransferStartMessageJsonBody() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_TRANSFER_START_TYPE)
                .build();
    }

    private JsonObject createTransferCompletionMessageJsonBody() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_TRANSFER_COMPLETION_TYPE)
                .build();
    }

    private JsonObject createTransferSuspensionMessageJsonBody() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_TRANSFER_SUSPENSION_TYPE)
                .build();
    }

    private JsonObject createTransferTerminationMessageJsonBody() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_TRANSFER_TERMINATION_TYPE)
                .build();
    }
}
