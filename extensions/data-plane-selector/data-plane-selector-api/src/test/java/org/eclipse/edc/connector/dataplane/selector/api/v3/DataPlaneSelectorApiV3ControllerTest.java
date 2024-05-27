/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.api.v3;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.dataplane.selector.api.model.SelectionRequest;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstance;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@ComponentTest
@ExtendWith(EdcExtension.class)
public class DataPlaneSelectorApiV3ControllerTest {

    private static final int PORT = 8181;

    @Test
    void getAll(DataPlaneInstanceStore store) {
        var list = List.of(createInstance("test-id1"), createInstance("test-id2"), createInstance("test-id3"));
        saveInstances(list, store);

        var array = baseRequest()
                .get()
                .then()
                .statusCode(200)
                .extract().body().as(JsonArray.class);

        assertThat(array).hasSize(3)
                .allSatisfy(jo -> assertThat(jo.asJsonObject().getString(ID))
                        .isIn("test-id1", "test-id2", "test-id3"));
    }

    @Test
    void getAll_noneExist() {
        var array = baseRequest()
                .get()
                .then()
                .statusCode(200)
                .extract().body().as(JsonArray.class);

        assertThat(array).isNotNull().isEmpty();
    }

    public JsonObject createSelectionRequestJson(String srcType, String destType, String strategy, String transferType) {
        var builder = createObjectBuilder()
                .add(SelectionRequest.SOURCE_ADDRESS, createDataAddress(srcType))
                .add(SelectionRequest.DEST_ADDRESS, createDataAddress(destType))
                .add(SelectionRequest.TRANSFER_TYPE, transferType);

        if (strategy != null) {
            builder.add(SelectionRequest.STRATEGY, strategy);
        }
        return builder.build();
    }

    protected RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .baseUri("http://localhost:" + PORT + "/api/v3/dataplanes")
                .when();
    }

    private JsonObjectBuilder createDataAddress(String type) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, type);
    }

    private void saveInstances(List<DataPlaneInstance> instances, DataPlaneInstanceStore store) {
        for (var instance : instances) {
            store.create(instance);
        }
    }
}
