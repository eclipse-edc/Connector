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
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Asset V3 endpoints end-to-end tests
 */
public class AssetApiEndToEndTest {

    @Nested
    @EndToEndTest
    class InMemory extends Tests implements InMemoryRuntime {

        InMemory() {
            super(RUNTIME);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests implements PostgresRuntime {

        Postgres() {
            super(RUNTIME);
        }

        @BeforeAll
        static void beforeAll() {
            PostgresqlEndToEndInstance.createDatabase("runtime");
        }
    }

    abstract static class Tests extends ManagementApiEndToEndTestBase {

        Tests(EdcRuntimeExtension runtime) {
            super(runtime);
        }

        @Test
        void getAssetById() {
            var id = UUID.randomUUID().toString();
            var asset = createAsset().id(id)
                    .dataAddress(createDataAddress().type("addressType").build())
                    .build();
            getAssetIndex().create(asset);

            var body = baseRequest()
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
        }

        @Test
        void createAsset_shouldBeStored() {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder().build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .build())
                    .build();

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            assertThat(getAssetIndex().findById(id)).isNotNull();
        }

        @Test
        void createAsset_shouldFail_whenBodyIsNotValid() {
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, " ")
                    .add("properties", createPropertiesBuilder().build())
                    .build();

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(400);
        }

        @Test
        void createAsset_withoutPrefix_shouldAddEdcNamespace() {
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

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var asset = getAssetIndex().findById(id);
            assertThat(asset).isNotNull();
            //make sure unprefixed keys are caught and prefixed with the EDC_NAMESPACE ns.
            assertThat(asset.getProperties().keySet())
                    .hasSize(6)
                    .allMatch(key -> key.startsWith(EDC_NAMESPACE));

            var dataAddress = getAssetIndex().resolveForAsset(asset.getId());
            assertThat(dataAddress).isNotNull();
            assertThat(dataAddress.getProperties().keySet())
                    .hasSize(2)
                    .allMatch(key -> key.startsWith(EDC_NAMESPACE));
        }

        @Test
        void queryAsset_byContentType() {
            //insert one asset into the index
            var id = UUID.randomUUID().toString();
            var asset = Asset.Builder.newInstance().id(id).contentType("application/octet-stream").dataAddress(createDataAddress().build()).build();
            getAssetIndex().create(asset);

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

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCustomStringProperty() {
            getAssetIndex().create(Asset.Builder.newInstance()
                    .id("test-asset")
                    .contentType("application/octet-stream")
                    .property("myProp", "myVal")
                    .dataAddress(createDataAddress().build())
                    .build());

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(query(criterion("myProp", "=", "myVal")))
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCustomComplexProperty() {
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

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v3/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var query = query(
                    criterion("'%sid".formatted(EDC_NAMESPACE), "=", id),
                    criterion("'%snested'.@id".formatted(EDC_NAMESPACE), "=", "test-nested-id")
            );

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v3/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void updateAsset() {
            var asset = createAsset().build();
            getAssetIndex().create(asset);

            var assetJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "Asset")
                    .add(ID, asset.getId())
                    .add("properties", createPropertiesBuilder()
                            .add("some-new-property", "some-new-value").build())
                    .add("dataAddress", createObjectBuilder()
                            .add("type", "addressType"))
                    .build();

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v3/assets")
                    .then()
                    .log().all()
                    .statusCode(204)
                    .body(notNullValue());

            var dbAsset = getAssetIndex().findById(asset.getId());
            assertThat(dbAsset).isNotNull();
            assertThat(dbAsset.getProperties()).containsEntry(EDC_NAMESPACE + "some-new-property", "some-new-value");
            assertThat(dbAsset.getDataAddress().getType()).isEqualTo("addressType");
        }

        private AssetIndex getAssetIndex() {
            return runtime.getContext().getService(AssetIndex.class);
        }

        private DataAddress.Builder createDataAddress() {
            return DataAddress.Builder.newInstance().type("test-type");
        }

        private Asset.Builder createAsset() {
            return Asset.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .name("test-asset")
                    .description("test description")
                    .contentType("application/json")
                    .version("0.4.2")
                    .dataAddress(createDataAddress().build());
        }

        private JsonObjectBuilder createPropertiesBuilder() {
            return createObjectBuilder()
                    .add("name", "test-asset")
                    .add("description", "test description")
                    .add("version", "0.4.2")
                    .add("contentType", "application/json");
        }

    }

}
