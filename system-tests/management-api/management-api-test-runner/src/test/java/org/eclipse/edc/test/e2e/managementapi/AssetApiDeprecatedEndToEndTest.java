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

import io.restassured.http.ContentType;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * V2 end-to-end tests
 *
 * @deprecated can be removed when Asset v2 endpoints will be removed.
 */
@EndToEndTest
@Deprecated(since = "0.1.2")
public class AssetApiDeprecatedEndToEndTest extends BaseManagementApiEndToEndTest {

    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final String TEST_ASSET_CONTENTTYPE = "application/json";
    private static final String TEST_ASSET_DESCRIPTION = "test description";
    private static final String TEST_ASSET_VERSION = "0.4.2";
    private static final String TEST_ASSET_NAME = "test-asset";

    @Test
    void getAssetById() {
        //insert one asset into the index
        controlPlane.getContext().getService(AssetIndex.class)
                .create(new AssetEntry(createAsset().build(),
                        createDataAddress().build()));

        var body = baseRequest()
                .get("/v2/assets/" + TEST_ASSET_ID)
                .then()
                .statusCode(200)
                .extract().body().jsonPath();

        assertThat(body).isNotNull();
        assertThat(body.getString(ID)).isEqualTo(TEST_ASSET_ID);
        assertThat(body.getMap("edc:properties"))
                .hasSize(5)
                .containsEntry("edc:name", TEST_ASSET_NAME)
                .containsEntry("edc:description", TEST_ASSET_DESCRIPTION)
                .containsEntry("edc:contenttype", TEST_ASSET_CONTENTTYPE)
                .containsEntry("edc:version", TEST_ASSET_VERSION);
    }

    @Test
    void createAsset_shouldBeStored() {

        var assetJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "Asset")
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().build())
                .build();

        var dataAddressJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "DataAddress")
                .add("type", "test-type").build();

        var assetNewJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "AssetEntryDto")
                .add("asset", assetJson)
                .add("dataAddress", dataAddressJson)
                .build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(assetNewJson)
                .post("/v2/assets")
                .then()
                .log().ifError()
                .statusCode(200)
                .body(ID, is("test-asset-id"));
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);

        assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);
        assertThat(assetIndex.findById("test-asset-id")).isNotNull();
    }

    @Test
    void createAsset_shouldFail_whenBodyIsNotValid() {
        var assetJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "Asset")
                .add(ID, " ")
                .add("properties", createPropertiesBuilder().build())
                .build();

        var assetNewJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "AssetEntryDto")
                .add("asset", assetJson)
                .build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(assetNewJson)
                .post("/v2/assets")
                .then()
                .log().ifError()
                .statusCode(400);

        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);

        assertThat(assetIndex.countAssets(emptyList())).isEqualTo(0);
    }

    @Test
    void createAsset_withoutPrefix_shouldAddEdcNamespace() {
        var assetJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "Asset")
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder()
                        .add("unprefixed-key", "test-value").build())
                .build();

        var dataAddressJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "DataAddress")
                .add("type", "test-type")
                .add("unprefixed-key", "test-value").build();

        var assetNewJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "AssetEntryDto")
                .add("asset", assetJson)
                .add("dataAddress", dataAddressJson)
                .build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(assetNewJson)
                .post("/v2/assets")
                .then()
                .log().ifError()
                .statusCode(200)
                .body(ID, is("test-asset-id"));
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);

        assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);
        var asset = assetIndex.findById("test-asset-id");
        assertThat(asset).isNotNull();
        //make sure unprefixed keys are caught and prefixed with the EDC_NAMESPACE ns.
        assertThat(asset.getProperties().keySet())
                .hasSize(6)
                .allMatch(key -> key.startsWith(EDC_NAMESPACE));

        var dataAddress = assetIndex.resolveForAsset(asset.getId());
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getProperties().keySet())
                .hasSize(2)
                .allMatch(key -> key.startsWith(EDC_NAMESPACE));
    }

    @Test
    void queryAsset_byContentType() {
        //insert one asset into the index
        controlPlane.getContext().getService(AssetIndex.class)
                .create(new AssetEntry(Asset.Builder.newInstance().id("test-asset").contentType("application/octet-stream").build(),
                        createDataAddress().build()));

        var query = createObjectBuilder()
                        .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                        .add("filterExpression", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("operandLeft", EDC_NAMESPACE + "contenttype")
                                        .add("operator", "=")
                                        .add("operandRight", "application/octet-stream"))
                        ).build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/v2/assets/request")
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
                createDataAddress().build()));

        var query = createSingleFilterQuery("myProp", "=", "myVal");

        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/v2/assets/request")
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
                createDataAddress().build()));

        var query = createSingleFilterQuery("myProp.description", "=", "test desc");

        // querying custom complex types in "json-path" style is expected not to work.
        baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/v2/assets/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void updateAsset() {
        var asset = createAsset();
        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);
        assetIndex.create(new AssetEntry(asset.build(), createDataAddress().build()));

        var assetJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "Asset")
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder()
                        .add("some-new-property", "some-new-value").build())
                .build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(assetJson)
                .put("/v2/assets")
                .then()
                .log().all()
                .statusCode(204)
                .body(notNullValue());

        var dbAsset = assetIndex.findById(TEST_ASSET_ID);
        assertThat(dbAsset).isNotNull();
        assertThat(dbAsset.getProperties()).containsEntry(EDC_NAMESPACE + "some-new-property", "some-new-value");
    }

    @Test
    void getDataAddress() {
        controlPlane.getContext().getService(AssetIndex.class)
                .create(new AssetEntry(Asset.Builder.newInstance().id("test-asset").build(),
                        DataAddress.Builder.newInstance().type("test-type").property(EDC_NAMESPACE + "another-key", "another-value").build()));

        baseRequest()
                .get("/v2/assets/test-asset/dataaddress")
                .then()
                .statusCode(200)
                .body("'edc:type'", equalTo("test-type"))
                .body("'edc:another-key'", equalTo("another-value"));
    }

    private DataAddress.Builder createDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type");
    }

    private Asset.Builder createAsset() {
        return Asset.Builder.newInstance()
                .id(TEST_ASSET_ID)
                .name(TEST_ASSET_NAME)
                .description(TEST_ASSET_DESCRIPTION)
                .contentType(TEST_ASSET_CONTENTTYPE)
                .version(TEST_ASSET_VERSION);
    }

    private JsonObjectBuilder createPropertiesBuilder() {
        return createObjectBuilder()
                .add("name", TEST_ASSET_NAME)
                .add("description", TEST_ASSET_DESCRIPTION)
                .add("version", TEST_ASSET_VERSION)
                .add("contentType", TEST_ASSET_CONTENTTYPE);
    }

    private JsonObject createSingleFilterQuery(String leftOperand, String operator, String rightOperand) {
        var criteria = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(TYPE, "Criterion")
                        .add("operandLeft", leftOperand)
                        .add("operator", operator)
                        .add("operandRight", rightOperand)
                );

        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "QuerySpec")
                .add("filterExpression", criteria)
                .build();
    }

    private record TestObject(String description, int number) { }
}
