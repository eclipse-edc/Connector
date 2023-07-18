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

package org.eclipse.edc.connector.dataplane.selector.api;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstance;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstanceBuilder;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstanceJson;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createSelectionRequestJson;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.hamcrest.Matchers.equalTo;

@ComponentTest
@ExtendWith(EdcExtension.class)
public class DataPlaneSelectorApiControllerTest {
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

    @Test
    void addEntry(DataPlaneInstanceStore store) {
        var dpi = createInstanceJson("test-id");

        baseRequest()
                .body(dpi)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(204);

        assertThat(store.getAll()).hasSize(1)
                .allMatch(d -> d.getId().equals("test-id"));
    }

    @Test
    void addEntry_exists_shouldOverwrite(DataPlaneInstanceStore store) {
        var dpi1 = createInstance("test-id1");
        var dpi2 = createInstance("test-id2");
        var dpi3 = createInstance("test-id3");
        saveInstances(List.of(dpi1, dpi2, dpi3), store);


        var newDpi = Json.createObjectBuilder(createInstanceJson("test-id2"))
                .add(DataPlaneInstance.ALLOWED_DEST_TYPES, Json.createValue("test-dest-type"))
                .build();

        baseRequest()
                .body(newDpi)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(204);


        assertThat(store.getAll()).hasSize(3)
                .contains(dpi1, dpi3)
                .doesNotContain(dpi2);
    }

    @Test
    void select(DataPlaneInstanceStore store) {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src2")
                .allowedDestType("test-dst2")
                .build();
        saveInstances(List.of(dpi, dpi2), store);

        var rq = createSelectionRequestJson("test-src1", "test-dst1");

        var result = baseRequest()
                .body(rq)
                .contentType(JSON)
                .post("/select")
                .then()
                .statusCode(200)
                .extract().body().as(JsonObject.class);
        assertThat(result.getString(ID)).isEqualTo("test-id");
    }

    @Test
    void select_noneCanHandle_shouldReturnEmpty(DataPlaneInstanceStore store) {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src2")
                .allowedDestType("test-dst2")
                .build();
        saveInstances(List.of(dpi, dpi2), store);

        var rq = createSelectionRequestJson("notexist-src", "test-dst2");


        baseRequest()
                .body(rq)
                .contentType(JSON)
                .post("/select")
                .then()
                .statusCode(204)
                .body(equalTo(""));
    }

    @Test
    void select_selectionStrategyNotFound(DataPlaneInstanceStore store) throws IOException {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src2")
                .allowedDestType("test-dst2")
                .build();
        saveInstances(List.of(dpi, dpi2), store);

        var rq = createSelectionRequestJson("test-src1", "test-dst2", "notexist");


        baseRequest()
                .body(rq)
                .contentType(JSON)
                .post("/select")
                .then()
                .statusCode(400);
    }

    @Test
    void select_withCustomStrategy(DataPlaneInstanceStore store, SelectionStrategyRegistry selectionStrategyRegistry) throws IOException {
        var dpi = createInstanceBuilder("test-id1")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id2")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        saveInstances(List.of(dpi, dpi2), store);

        var myCustomStrategy = new SelectionStrategy() {
            @Override
            public DataPlaneInstance apply(List<DataPlaneInstance> instances) {
                return instances.stream().filter(i -> i.getId().equals("test-id1")).findFirst().get();
            }

            @Override
            public String getName() {
                return "myCustomStrategy";
            }
        };

        selectionStrategyRegistry.add(myCustomStrategy);

        var rq = createSelectionRequestJson("test-src1", "test-dst1", "myCustomStrategy");

        var result = baseRequest()
                .body(rq)
                .contentType(JSON)
                .post("/select")
                .then()
                .statusCode(200)
                .extract().body().as(JsonObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("test-id1");
    }

    protected RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .baseUri("http://localhost:" + PORT + "/api/instances")
                .when();
    }

    private void saveInstances(List<DataPlaneInstance> instances, DataPlaneInstanceStore store) {
        for (var instance : instances) {
            store.create(instance);
        }
    }
}
