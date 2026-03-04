/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.catalog.api.query;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.catalog.test.TestUtil;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.v2025.from.JsonObjectFromCatalogV2025Transformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.catalog.test.TestUtil.buildCatalog;
import static org.eclipse.edc.catalog.test.TestUtil.createCatalog;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class FederatedCatalogApiControllerTest extends RestControllerTestBase {

    private static final String PATH = "/v1alpha/catalog/query";
    private final QueryService queryService = mock();

    @Test
    void queryApi_whenEmptyResult() {
        when(queryService.getCatalog(any())).thenReturn(ServiceResult.success(emptyList()));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post(PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));
    }

    @Test
    void queryApi_whenResultsReturned() {
        var catalogs = range(0, 3).mapToObj(i -> createCatalog("catalog-" + i)).toList();
        when(queryService.getCatalog(any())).thenReturn(ServiceResult.success(catalogs));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post(PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(3));
    }

    @Test
    void queryApi_whenQueryUnsuccessful() {
        when(queryService.getCatalog(any())).thenThrow(new RuntimeException("test exception"));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post(PATH)
                .then()
                .statusCode(500);
    }

    @Test
    void queryApi_whenFlattened() {
        var catalogs = range(0, 2).mapToObj(i ->
                buildCatalog("catalog-" + i)
                        .dataset(createCatalog("sub1-" + i))
                        .dataset(buildCatalog("sub2-" + i)
                                .dataset(createCatalog("subsub1-" + i))
                                .dataset(Dataset.Builder.newInstance().id("sub2-normal-asset-" + i).build())
                                .build())
                        .build()).toList();
        when(queryService.getCatalog(any())).thenReturn(ServiceResult.success(catalogs));

        var response = baseRequest()
                .contentType(JSON)
                .body("{}")
                .post(PATH + "?flatten=true")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(2))
                .body("[0].'http://www.w3.org/ns/dcat#dataset'", hasSize(3))
                .body("[1].'http://www.w3.org/ns/dcat#dataset'", hasSize(3));

        var jsonPath = response.extract().body().jsonPath();
        List<String> types1 = jsonPath.get("[0].'http://www.w3.org/ns/dcat#dataset'.'@type'");
        assertThat(types1).containsOnly("http://www.w3.org/ns/dcat#Dataset");

        List<String> types2 = jsonPath.get("[1].'http://www.w3.org/ns/dcat#dataset'.'@type'");
        assertThat(types2).containsOnly("http://www.w3.org/ns/dcat#Dataset");
    }

    @Test
    void queryApi_shouldUseEmptyQuerySpec_whenRequestBodyIsNull() {
        when(queryService.getCatalog(any())).thenReturn(ServiceResult.success(emptyList()));

        baseRequest()
                .contentType(JSON)
                .post(PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));

        verify(queryService).getCatalog(QuerySpec.none());
    }

    @Override
    protected Object controller() {
        var typeTransformerRegistry = new TypeTransformerRegistryImpl();
        var factory = Json.createBuilderFactory(Map.of());
        var mapper = new JacksonTypeManager();
        var participantIdMapper = new TestUtil.NoOpParticipantIdMapper();
        typeTransformerRegistry.register(new JsonObjectFromCatalogV2025Transformer(factory, new JacksonTypeManager(), JSON_LD, participantIdMapper, DSP_NAMESPACE_V_2025_1));
        typeTransformerRegistry.register(new JsonObjectFromDatasetTransformer(factory, mapper, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDistributionTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectFromDataServiceTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        return new FederatedCatalogApiController(queryService, typeTransformerRegistry);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
