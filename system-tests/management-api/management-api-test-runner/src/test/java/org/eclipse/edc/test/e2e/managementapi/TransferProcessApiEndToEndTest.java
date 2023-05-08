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

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class TransferProcessApiEndToEndTest extends BaseManagementApiEndToEndTest {

    @Test
    void getAll() {
        getStore().updateOrCreate(createTransferProcess("tp1"));
        getStore().updateOrCreate(createTransferProcess("tp2"));
        var requestBody = Json.createObjectBuilder().build();

        baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].@id", anyOf(is("tp1"), is("tp2")))
                .body("[1].@id", anyOf(is("tp1"), is("tp2")));
    }

    @Test
    void getById() {
        getStore().updateOrCreate(createTransferProcess("tp1"));
        getStore().updateOrCreate(createTransferProcess("tp2"));

        baseRequest()
                .get("/tp2")
                .then()
                .statusCode(200)
                .body("@id", is("tp2"))
                .body(TYPE, is("edc:TransferProcessDto"));
    }

    @Test
    void getState() {
        getStore().updateOrCreate(createTransferProcessBuilder("tp2").state(COMPLETED.code()).build());

        baseRequest()
                .get("/tp2/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is("edc:TransferState"))
                .body("'edc:state'", is("COMPLETED"));
    }

    @Test
    void create() {
        var requestBody = Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "TransferRequestDto")
                .add(EDC_NAMESPACE + "dataDestination", Json.createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "DataAddress")
                        .add(EDC_NAMESPACE + "type", "HttpData")
                        .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                                .add(EDC_NAMESPACE + "baseUrl", "http://any")
                                .build())
                        .build()
                )
                .add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http")
                .add(EDC_NAMESPACE + "assetId", "assetId")
                .build();

        var id = baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .post("/")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        assertThat(getStore().findById(id)).isNotNull();
    }

    @Test
    void deprovision() {
        var id = UUID.randomUUID().toString();
        getStore().updateOrCreate(createTransferProcessBuilder(id).state(COMPLETED.code()).build());

        baseRequest()
                .contentType(JSON)
                .post("/" + id + "/deprovision")
                .then()
                .statusCode(204);
    }

    @Test
    void terminate() {
        var id = UUID.randomUUID().toString();
        getStore().updateOrCreate(createTransferProcess(id));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().build())
                .post("/" + id + "/terminate")
                .then()
                .statusCode(204);
    }

    private TransferProcessStore getStore() {
        return controlPlane.getContext().getService(TransferProcessStore.class);
    }

    private TransferProcess createTransferProcess(String id) {
        return createTransferProcessBuilder(id).build();
    }

    private TransferProcess.Builder createTransferProcessBuilder(String id) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(DataRequest.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .dataDestination(DataAddress.Builder.newInstance()
                                .type("type")
                                .build())
                        .protocol("dataspace-protocol-http")
                        .processId(id)
                        .build());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + PORT + "/management/v2/transferprocesses")
                .when();
    }
}
