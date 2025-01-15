/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.api.v4;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstance;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
public class DataPlaneSelectorApiV4ControllerTest extends RestControllerTestBase {

    private final DataPlaneSelectorService selectionService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    @Test
    void getAll() {
        var list = List.of(createInstance("test-id1"), createInstance("test-id2"), createInstance("test-id3"));
        when(selectionService.getAll()).thenReturn(ServiceResult.success(list));
        when(transformerRegistry.transform(isA(DataPlaneInstance.class), eq(JsonObject.class)))
                .thenAnswer(i -> Result.success(jsonInstance(i.getArgument(0, DataPlaneInstance.class).getId())));

        var array = baseRequest()
                .get()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(JsonArray.class);

        assertThat(array).hasSize(3)
                .allSatisfy(jo -> assertThat(jo.asJsonObject().getString(ID))
                        .isIn("test-id1", "test-id2", "test-id3"));
    }

    @Test
    void getAll_noneExist() {
        when(selectionService.getAll()).thenReturn(ServiceResult.success(emptyList()));

        var array = baseRequest()
                .get()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(JsonArray.class);

        assertThat(array).isNotNull().isEmpty();
    }

    protected RequestSpecification baseRequest() {
        return given()
                .port(port)
                .basePath("/v4alpha/dataplanes")
                .when();
    }

    private JsonObject jsonInstance(String id) {
        return Json.createObjectBuilder().add(ID, id).build();
    }

    @Override
    protected Object controller() {
        return new DataplaneSelectorApiV4Controller(selectionService, transformerRegistry);
    }
}
