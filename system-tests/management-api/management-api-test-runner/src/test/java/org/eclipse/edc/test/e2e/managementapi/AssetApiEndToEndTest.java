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
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Asset V3 endpoints end-to-end tests
 */
public class AssetApiEndToEndTest {

    abstract static class Tests {

        @Test
        void getAssetById(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var asset = createAsset().id(id)
                    .dataAddress(createDataAddress().type("addressType").build())
                    .build();
            assetIndex.create(asset);

            var body = context.baseRequest()
                    .get("/v3/assets/" + id)
                    .then()
                    .statusCode(200)
                    .extract().body().jsonPath();

            assertThat(body).isNotNull();
            assertThat(body.getString(ID)).isEqualTo(id);
            assertThat(body.getMap("properties"))
                    .hasSize(5)
                    .containsEntry("name", "test-asset")
                    .containsEntry("description", "test description")
                    .containsEntry("contenttype", "application/json")
                    .containsEntry("version", "0.4.2");
            assertThat(body.getMap("'dataAddress'"))
                    .containsEntry("type", "addressType");
            assertThat(body.getMap("'dataAddress'.'complex'"))
                    .containsEntry("simple", "value")
                    .containsKey("nested");
            assertThat(body.getMap("'dataAddress'.'complex'.'nested'"))
                    .containsEntry("innerValue", "value");
        }

        @Test
        void createAsset_shouldBeStored(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder().add("isCatalog", "true").build())
                    .add("privateProperties", createObjectBuilder()
                            .add("anotherProp", "anotherVal")
                            .build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .add("complex", createObjectBuilder()
                                    .add("simple", "value")
                                    .add("nested", createObjectBuilder()
                                            .add("innerValue", "value"))
                                    .build())
                            .build())
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var asset = assetIndex.findById(id);
            assertThat(asset).isNotNull();
            assertThat(asset.isCatalog()).isTrue();
            assertThat(asset.getPrivateProperty(EDC_NAMESPACE + "anotherProp")).isEqualTo("anotherVal");
            assertThat(asset.getDataAddress().getProperty("complex"))
                    .asInstanceOf(MAP)
                    .containsEntry(EDC_NAMESPACE + "simple", List.of(Map.of(VALUE, "value")))
                    .containsEntry(EDC_NAMESPACE + "nested", List.of(Map.of(EDC_NAMESPACE + "innerValue", List.of(Map.of(VALUE, "value")))));

            assertThat(asset.getParticipantContextId()).isNotNull();
        }

        @Test
        void createAsset_shouldFail_whenBodyIsNotValid(ManagementEndToEndTestContext context) {
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, " ")
                    .add("properties", createPropertiesBuilder().build())
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(400);
        }

        @Test
        void createAsset_withoutPrefix_shouldAddEdcNamespace(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder()
                            .add("unprefixed-key", "test-value").build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .add("unprefixed-key", "test-value")
                            .build())
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var asset = assetIndex.findById(id);
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
        void createAsset_whenCatalogAsset_shouldSetProperty(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "CatalogAsset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder().build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .build())
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var asset = assetIndex.findById(id);
            assertThat(asset).isNotNull();
            assertThat(asset.isCatalog()).isTrue();
        }


        @Test
        void createAsset_whenCatalogInPrivateProps_shouldReturnCatalogType(ManagementEndToEndTestContext context, AssetIndex index) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder().add("isCatalog", "true").build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .build())
                    .build();

            // create the asset
            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            // verify the property was set
            var asset = index.findById(id);
            assertThat(asset.isCatalog()).isTrue();

            // query the asset, assert that @type: CatalogAsset
            var assets = context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(context.query(criterion("id", "=", id)))
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(JsonArray.class);

            assertThat(assets).isNotNull().hasSize(1);
            assertThat(Asset.EDC_CATALOG_ASSET_TYPE).contains(assets.get(0).asJsonObject().getString(TYPE));
        }

        @Test
        void queryAsset_byContentType(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            //insert one asset into the index
            var id = UUID.randomUUID().toString();
            var asset = Asset.Builder.newInstance().id(id).contentType("application/octet-stream").dataAddress(createDataAddress().build()).participantContextId("participantContextId").build();
            assetIndex.create(asset);

            var query = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add("filterExpression", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("operandLeft", EDC_NAMESPACE + "id")
                                    .add("operator", "=")
                                    .add("operandRight", id))
                            .add(createObjectBuilder()
                                    .add("operandLeft", EDC_NAMESPACE + "contenttype")
                                    .add("operator", "=")
                                    .add("operandRight", "application/octet-stream"))
                    ).build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCustomStringProperty(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            assetIndex.create(Asset.Builder.newInstance()
                    .id("test-asset")
                    .contentType("application/octet-stream")
                    .property("myProp", "myVal")
                    .dataAddress(createDataAddress().build())
                    .participantContextId("participantContextId")
                    .build());

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(context.query(criterion("myProp", "=", "myVal")))
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCustomComplexProperty(ManagementEndToEndTestContext context) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder()
                            .add("nested", createPropertiesBuilder()
                                    .add("@id", "test-nested-id")))
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .add("unprefixed-key", "test-value")
                            .build())
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var query = context.query(
                    criterion("'%sid".formatted(EDC_NAMESPACE), "=", id),
                    criterion("'%snested'.@id".formatted(EDC_NAMESPACE), "=", "test-nested-id")
            );

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCatalogProperty(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            assetIndex.create(Asset.Builder.newInstance()
                    .property(Asset.PROPERTY_IS_CATALOG, true)
                    .id(id)
                    .contentType("application/octet-stream")
                    .dataAddress(createDataAddress().build())
                    .participantContextId("participantContextId")
                    .build());

            var assets = context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(context.query(
                            criterion(EDC_NAMESPACE + "isCatalog", "=", "true"),
                            criterion("id", "=", id)))
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(JsonArray.class);

            assertThat(assets).isNotNull().hasSize(1);
            assertThat(Asset.EDC_CATALOG_ASSET_TYPE).contains(assets.get(0).asJsonObject().getString(TYPE));

        }

        @Test
        void updateAsset(ManagementEndToEndTestContext context, AssetIndex assetIndex) {
            var asset = createAsset().build();
            assetIndex.create(asset);

            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, asset.getId())
                    .add("properties", createPropertiesBuilder()
                            .add("some-new-property", "some-new-value").build())
                    .add("dataAddress", createObjectBuilder()
                            .add("type", "addressType")
                            .add("complex", createObjectBuilder().add("nested", "value").build()))
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v3/assets")
                    .then()
                    .log().all()
                    .statusCode(204)
                    .body(notNullValue());

            var dbAsset = assetIndex.findById(asset.getId());
            assertThat(dbAsset).isNotNull();
            assertThat(dbAsset.getProperties()).containsEntry(EDC_NAMESPACE + "some-new-property", "some-new-value");
            assertThat(dbAsset.getDataAddress().getType()).isEqualTo("addressType");
            assertThat(dbAsset.getDataAddress().getProperty(EDC_NAMESPACE + "complex"))
                    .asInstanceOf(MAP)
                    .containsEntry(EDC_NAMESPACE + "nested", List.of(Map.of(VALUE, "value")));
        }

        private DataAddress.Builder createDataAddress() {
            return DataAddress.Builder.newInstance().type("test-type")
                    .property(EDC_NAMESPACE + "complex", Map.of(
                            EDC_NAMESPACE + "simple", "value",
                            EDC_NAMESPACE + "nested", Map.of(EDC_NAMESPACE + "innerValue", "value")
                    ));
        }

        private Asset.Builder createAsset() {
            return Asset.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .name("test-asset")
                    .description("test description")
                    .contentType("application/json")
                    .version("0.4.2")
                    .dataAddress(createDataAddress().build())
                    .participantContextId("participantContextId");
        }

        private JsonObjectBuilder createPropertiesBuilder() {
            return createObjectBuilder()
                    .add("name", "test-asset")
                    .add("description", "test description")
                    .add("version", "0.4.2")
                    .add("contentType", "application/json");
        }

    }

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static ManagementEndToEndExtension runtime = new ManagementEndToEndExtension.Postgres(postgres);

    }

}
