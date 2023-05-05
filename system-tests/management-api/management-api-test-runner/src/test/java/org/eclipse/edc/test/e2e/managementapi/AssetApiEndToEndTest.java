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

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_ASSET_TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class AssetApiEndToEndTest extends BaseManagementApiEndToEndTest {

    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final String TEST_ASSET_CONTENTTYPE = "application/json";
    private static final String TEST_ASSET_DESCRIPTION = "test description";
    private static final String TEST_ASSET_VERSION = "0.4.2";
    private static final String TEST_ASSET_NAME = "test-asset";

    @Test
    void getAssetById() {
        //insert one asset into the index
        controlPlane.getContext().getService(AssetIndex.class)
                .create(new AssetEntry(Asset.Builder.newInstance().id("test-asset").build(),
                        DataAddress.Builder.newInstance().type("test-type").build()));

        var body = baseRequest()
                .get("/test-asset")
                .then()
                .statusCode(200)
                .extract().body().as(new TypeRef<Map<String, Object>>() {
                });

        assertThat(body).isNotNull();
        assertThat(body).containsKey("asset");
        assertThat(body.get("asset")).isInstanceOf(Map.class);
        Map<String, Object> asset = (Map<String, Object>) body.get("asset");
        assertThat(asset.get(Asset.PROPERTY_ID).toString()).contains("test-asset");
    }

    @Test
    void createAsset_shouldBeStored() {

        var assetJson = Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().build())
                .build();

        var json = Map.of("asset", assetJson,
                "dataAddress", Map.of("properties", Map.of("type", "test-type")));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(json)
                .post()
                .then()
                .log().ifError()
                .statusCode(200)
                .body("id", is("test-asset-id"));
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);

        assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);
        assertThat(assetIndex.findById("test-asset-id")).isNotNull();
    }

    @Test
    void queryAsset_byContentType() {
        //insert one asset into the index
        controlPlane.getContext().getService(AssetIndex.class)
                .create(new AssetEntry(Asset.Builder.newInstance().id("test-asset").contentType("application/octet-stream").build(),
                        DataAddress.Builder.newInstance().type("test-type").build()));

        // create the query by content type
        //TODO: once the queryspec dto is JSON-LD aware, we can just use "contentype", and the JSON-LD expansion takes care of prefixing the namespace
        var byContentType = CriterionDto.Builder.newInstance().operandLeft(Asset.PROPERTY_CONTENT_TYPE).operator("=").operandRight("application/octet-stream").build();
        var query = QuerySpecDto.Builder.newInstance().filterExpression(List.of(byContentType)).build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("size()", is(1));

    }

    @Test
    void queryAsset_byCustomStringProperty() {
        //insert one asset into the index
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);
        assetIndex.create(new AssetEntry(Asset.Builder.newInstance()
                .id("test-asset")
                .contentType("application/octet-stream")
                .property("myProp", "myVal")
                .build(),
                DataAddress.Builder.newInstance().type("test-type").build()));

        var byCustomProp = CriterionDto.Builder.newInstance()
                .operandLeft("myProp")
                .operator("=")
                .operandRight("myVal").build();
        var query = QuerySpecDto.Builder.newInstance().filterExpression(List.of(byCustomProp)).build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    void queryAsset_byCustomComplexProperty_whenJsonPathQuery_expectNoResult() {
        //insert one asset into the index
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);
        assetIndex.create(new AssetEntry(Asset.Builder.newInstance()
                .id("test-asset")
                .contentType("application/octet-stream")
                // use a custom, complex object type
                .property("myProp", new TestObject("test desc", 42))
                .build(),
                DataAddress.Builder.newInstance().type("test-type").build()));

        var byCustomProp = CriterionDto.Builder.newInstance()
                .operandLeft("myProp.description") //access in "json-path style", will not work
                .operator("=")
                .operandRight("test desc").build();
        var query = QuerySpecDto.Builder.newInstance().filterExpression(List.of(byCustomProp)).build();

        // querying custom complex types in "json-path" style is expected not to work.
        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void queryAsset_byCustomComplexProperty_whenLikeOperator_expectException() {
        //insert one asset into the index
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);
        assetIndex.create(new AssetEntry(Asset.Builder.newInstance()
                .id("test-asset")
                .contentType("application/octet-stream")
                // use a custom, complex object type
                .property("myProp", new TestObject("test desc", 42))
                .build(),
                DataAddress.Builder.newInstance().type("test-type").build()));

        var byCustomProp = CriterionDto.Builder.newInstance()
                .operandLeft("myProp") //access with LIKE operator, will not work, because the inmem query convert does not support this
                .operator("LIKE")
                .operandRight("test desc").build();
        var query = QuerySpecDto.Builder.newInstance().filterExpression(List.of(byCustomProp)).build();

        // querying custom complex types in "json-path" style is expected not to work.
        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(500);
    }

    private JsonObjectBuilder createPropertiesBuilder() {
        return Json.createObjectBuilder()
                .add("name", TEST_ASSET_NAME)
                .add("description", TEST_ASSET_DESCRIPTION)
                .add("edc:version", TEST_ASSET_VERSION)
                .add("contenttype", TEST_ASSET_CONTENTTYPE);
    }

    private JsonObjectBuilder createContextBuilder() {
        return Json.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + PORT + "/management/v2/assets")
                .when();
    }

    private static class TestObject {
        private final String description;
        private final int number;

        TestObject(String description, int number) {
            this.description = description;
            this.number = number;
        }

        public String getDescription() {
            return description;
        }

        public int getNumber() {
            return number;
        }
    }
}
