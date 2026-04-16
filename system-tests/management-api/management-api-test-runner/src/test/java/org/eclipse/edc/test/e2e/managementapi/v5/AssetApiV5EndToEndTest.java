/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi.v5;

import io.restassured.http.ContentType;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.participantContext;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Asset v5beta endpoints end-to-end tests
 */
public class AssetApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);

        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        @Test
        void updateAsset(ManagementEndToEndV5TestContext context, AssetIndex assetIndex) {
            var asset = createAsset().build();
            assetIndex.create(asset);

            var assetJson = createAssetJson(asset);

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().all()
                    .statusCode(204)
                    .body(notNullValue());

            var dbAsset = assetIndex.findById(asset.getId());
            assertThat(dbAsset).isNotNull();
            assertThat(dbAsset.getProperties()).containsEntry(EDC_NAMESPACE + "some-new-property",
                    "some-new-value");
            assertThat(dbAsset.getDataAddress().getType()).isEqualTo("addressType");
            assertThat(dbAsset.getDataAddress().getProperty(EDC_NAMESPACE + "complex"))
                    .asInstanceOf(MAP)
                    .containsEntry(EDC_NAMESPACE + "nested", List.of(Map.of(VALUE, "value")));

            assertThat(asset.getParticipantContextId()).isEqualTo(PARTICIPANT_CONTEXT_ID);
        }

        @Test
        void updateAsset_doesNotOwnResource(ManagementEndToEndV5TestContext context, AssetIndex assetIndex) {
            var asset = createAsset().build();
            assetIndex.create(asset);

            var assetJson = createAssetJson(asset);

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().all()
                    .statusCode(204)
                    .body(notNullValue());

            var dbAsset = assetIndex.findById(asset.getId());
            assertThat(dbAsset).isNotNull();
            assertThat(dbAsset.getProperties()).containsEntry(EDC_NAMESPACE + "some-new-property",
                    "some-new-value");
            assertThat(dbAsset.getDataAddress().getType()).isEqualTo("addressType");
            assertThat(dbAsset.getDataAddress().getProperty(EDC_NAMESPACE + "complex"))
                    .asInstanceOf(MAP)
                    .containsEntry(EDC_NAMESPACE + "nested", List.of(Map.of(VALUE, "value")));
        }

        @Test
        void updateAsset_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context,
                                                       OauthServer authServer,
                                                       AssetIndex assetIndex, ParticipantContextService srv) {
            var asset = createAsset().build();
            assetIndex.create(asset);

            var assetJson = createAssetJson(asset);

            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);
            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body(notNullValue());
        }

        @Test
        void updateAsset_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                 OauthServer authServer) {
            var asset = createAsset().build();
            assetIndex.create(asset);

            var assetJson = createAssetJson(asset);

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(notNullValue());
        }

        @Test
        void updateAsset_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context,
                                            OauthServer authServer,
                                            AssetIndex assetIndex) {
            var asset = createAsset().build();
            assetIndex.create(asset);

            var assetJson = createAssetJson(asset);

            var adminToken = authServer.createAdminToken();

            context.baseRequest(adminToken)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().all()
                    .statusCode(204)
                    .body(notNullValue());
        }

        @Test
        void queryAsset_byCustomStringProperty(ManagementEndToEndV5TestContext context, AssetIndex assetIndex) {
            assetIndex.create(Asset.Builder.newInstance()
                    .id("test-asset")
                    .property("myProp", "myVal")
                    .dataAddress(createDataAddress().build())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build());

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query(criterion("myProp", "=", "myVal")).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCustomComplexProperty(ManagementEndToEndV5TestContext context) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
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
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var query = context.query(
                            criterion("'%sid".formatted(EDC_NAMESPACE), "=", id),
                            criterion("'%snested'.@id".formatted(EDC_NAMESPACE), "=", "test-nested-id"))
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));
        }

        @Test
        void queryAsset_byCatalogProperty(ManagementEndToEndV5TestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            assetIndex.create(Asset.Builder.newInstance()
                    .property(Asset.PROPERTY_IS_CATALOG, true)
                    .id(id)
                    .dataAddress(createDataAddress().build())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build());

            var body = context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query(
                                    criterion(EDC_NAMESPACE + "isCatalog", "=", "true"),
                                    criterion("id", "=", id))
                            .toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body();

            var assets = body.as(Map[].class);

            assertThat(assets).isNotNull().hasSize(1);
            assertThat(Asset.EDC_CATALOG_ASSET_TYPE).contains(assets[0].get(TYPE).toString());
        }

        @Test
        void queryAsset_tokenBearerIsAdmin_shouldReturnAllAssets(ManagementEndToEndV5TestContext context,
                                                                 OauthServer authServer,
                                                                 AssetIndex assetIndex) {
            IntStream.range(0, 10)
                    .forEach(i -> {
                        // create assets for participant
                        assetIndex.create(Asset.Builder.newInstance()
                                        .id(UUID.randomUUID().toString())
                                        .property("quizz", "quazz")
                                        .dataAddress(createDataAddress().build())
                                        .participantContextId(PARTICIPANT_CONTEXT_ID)
                                        .build())
                                .orElseThrow(f -> new AssertionError(
                                        f.getFailureDetail()));

                    });

            var token = authServer.createAdminToken();

            var result = context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(context.query(criterion("quizz", "=", "quazz")).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(Map[].class);

            assertThat(result).isNotNull().hasSize(10);

        }

        @Test
        void queryAsset_shouldLimitToResourceOwner(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                   ParticipantContextService srv) {
            var otherParticipantId = UUID.randomUUID().toString();

            srv.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var ownAssetId = UUID.randomUUID().toString();
            var otherAssetId = UUID.randomUUID().toString();

            assetIndex.create(createAsset()
                            .id(ownAssetId)
                            .property("kind", "limit")
                            .participantContextId(PARTICIPANT_CONTEXT_ID)
                            .build())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            assetIndex.create(createAsset()
                            .id(otherAssetId)
                            .property("kind", "limit")
                            .participantContextId(otherParticipantId)
                            .build())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var query = context.query(criterion("kind", "=", "limit")).toString(); // empty query

            var result = context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(Map[].class);

            assertThat(result).isNotNull().hasSize(1)
                    .allMatch(m -> m.get("@id").equals(ownAssetId));

        }

        @Test
        void queryAsset_tokenBearerNotEqualResourceOwner(ManagementEndToEndV5TestContext context,
                                                         OauthServer authServer,
                                                         AssetIndex assetIndex, ParticipantContextService srv) {
            var participantId = UUID.randomUUID().toString();
            srv.createParticipantContext(participantContext(participantId))
                    .orElseThrow(f -> new AssertionError(
                            "ParticipantContext " + participantId + " not created."));

            var token = authServer.createToken(participantId);

            var id = UUID.randomUUID().toString();
            assetIndex.create(Asset.Builder.newInstance()
                    .id(id)
                    .property("foo", "bar")
                    .dataAddress(createDataAddress().build())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build());

            var query = context.query(criterion("foo", "=", "bar")).toString();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource."
                            .formatted(participantId)));

        }

        @Test
        void createAsset_shouldBeStored(ManagementEndToEndV5TestContext context, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
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
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
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
                    .containsEntry(EDC_NAMESPACE + "nested",
                            List.of(Map.of(EDC_NAMESPACE + "innerValue",
                                    List.of(Map.of(VALUE, "value")))));
            assertThat(asset.getParticipantContextId()).isEqualTo(PARTICIPANT_CONTEXT_ID);
        }

        @Test
        void createAsset_shouldFail_whenBodyIsNotValid(ManagementEndToEndV5TestContext context) {
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "Asset")
                    .add(ID, " ")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifError()
                    .statusCode(400);
        }

        @Test
        void createAsset_withoutPrefix_shouldAddEdcNamespace(ManagementEndToEndV5TestContext context,
                                                             AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder()
                            .add("unprefixed-key", "test-value").build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .add("unprefixed-key", "test-value")
                            .build())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var asset = assetIndex.findById(id);
            assertThat(asset).isNotNull();
            // make sure unprefixed keys are caught and prefixed with the EDC_NAMESPACE ns.
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
        void createAsset_whenCatalogAsset_shouldSetProperty(ManagementEndToEndV5TestContext context,
                                                            AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CatalogAsset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder().build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .build())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            var asset = assetIndex.findById(id);
            assertThat(asset).isNotNull();
            assertThat(asset.isCatalog()).isTrue();
        }

        @Test
        void createAsset_whenCatalogInPrivateProps_shouldReturnCatalogType(
                ManagementEndToEndV5TestContext context, AssetIndex index) {
            var id = UUID.randomUUID().toString();
            var assetJson = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "Asset")
                    .add(ID, id)
                    .add("properties", createPropertiesBuilder().add("isCatalog", "true").build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "test-type")
                            .build())
                    .build()
                    .toString();

            // create the asset
            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(assetJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            // verify the property was set
            var asset = index.findById(id);
            assertThat(asset.isCatalog()).isTrue();

            // query the asset, assert that @type: CatalogAsset
            var assets = context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query(criterion("id", "=", id)).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(Map[].class);

            assertThat(assets).isNotNull().hasSize(1);
            assertThat(Asset.EDC_CATALOG_ASSET_TYPE).contains(assets[0].get(TYPE).toString());
        }

        @Test
        void createAsset_tokenBearerWrong(ManagementEndToEndV5TestContext context,
                                          OauthServer authServer,
                                          ParticipantContextService service) {
            var json = createAssetJson(createAsset().build());
            var id = "other-participant";
            service.createParticipantContext(participantContext(id))
                    .orElseThrow(f -> new AssertionError(
                            "ParticipantContext " + id + " not created."));

            var token = authServer.createToken(id);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(json)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource."
                            .formatted(id)));
        }

        @Test
        void createAsset_tokenLacksWriteScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var json = createAssetJson(createAsset().build());

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID,
                    Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(json)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*missing.*"));
        }

        @Test
        void createAsset_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var json = createAssetJson(createAsset().build());

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(json)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);
        }

        @Test
        void createAsset_tokenBearerIsAdmin_participantNotFound(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var json = createAssetJson(createAsset().build());

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(json)
                    .post("/v5beta/participants/who-is-this/assets")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }

        @Test
        void findById(ManagementEndToEndV5TestContext context, AssetIndex assetIndex) {

            var id = UUID.randomUUID().toString();
            var asset = createAsset().id(id)
                    .dataAddress(createDataAddress().type("addressType").build())
                    .build();
            assetIndex.create(asset);

            var body = context.baseRequest(participantTokenJwt)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().jsonPath();

            assertThat(body).isNotNull();
            assertThat(body.getString(ID)).isEqualTo(id);
            assertThat(body.getMap("properties"))
                    .hasSize(2)
                    .containsEntry("description", "test description");
            assertThat(body.getMap("'dataAddress'"))
                    .containsEntry("type", "addressType");
            assertThat(body.getMap("'dataAddress'.'complex'"))
                    .containsEntry("simple", "value")
                    .containsKey("nested");
            assertThat(body.getMap("'dataAddress'.'complex'.'nested'"))
                    .containsEntry("innerValue", "value");
        }

        @Test
        void findById_assetNotFound(ManagementEndToEndV5TestContext context) {

            var id = "not-exist";
            context.baseRequest(participantTokenJwt)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        void findById_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context,
                                                    OauthServer authServer,
                                                    AssetIndex assetIndex, ParticipantContextService srv) {
            var id = UUID.randomUUID().toString();
            var asset = createAsset().id(id)
                    .dataAddress(createDataAddress().type("addressType").build())
                    .build();
            assetIndex.create(asset);

            var participantContextId = UUID.randomUUID().toString();
            createParticipant(srv, participantContextId);
            var token = authServer.createToken(participantContextId);

            var body = context.baseRequest(token)
                    .header("Authorization", "Bearer " + token)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401)
                    .extract().body().jsonPath();

            assertThat(body).isNotNull();
        }

        @Test
        void findById_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var asset = createAsset().id(id)
                    .dataAddress(createDataAddress().type("addressType").build())
                    .build();
            assetIndex.create(asset);

            var adminToken = authServer.createAdminToken();
            var body = context.baseRequest(adminToken)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/assets/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().jsonPath();

            assertThat(body).isNotNull();
            assertThat(body.getString(ID)).isEqualTo(id);
            assertThat(body.getMap("properties"))
                    .hasSize(2)
                    .containsEntry("description", "test description");
            assertThat(body.getMap("'dataAddress'"))
                    .containsEntry("type", "addressType");
            assertThat(body.getMap("'dataAddress'.'complex'"))
                    .containsEntry("simple", "value")
                    .containsKey("nested");
            assertThat(body.getMap("'dataAddress'.'complex'.'nested'"))
                    .containsEntry("innerValue", "value");
        }

        @Test
        void findById_tokenBearerIsAdmin_wrongOwner(ManagementEndToEndV5TestContext context,
                                                    OauthServer authServer,
                                                    AssetIndex assetIndex) {
            var id = UUID.randomUUID().toString();
            var asset = createAsset().id(id)
                    .dataAddress(createDataAddress().type("addressType").build())
                    .build();
            assetIndex.create(asset);

            var adminToken = authServer.createAdminToken();
            context.baseRequest(adminToken)
                    .get("/v5beta/participants/some-other-owner/assets/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .log().ifValidationFails();

        }

        private String createAssetJson(Asset asset) {
            return createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "Asset")
                    .add(ID, asset.getId())
                    .add("properties", createPropertiesBuilder()
                            .add("some-new-property", "some-new-value").build())
                    .add("dataAddress", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "addressType")
                            .add("complex", createObjectBuilder().add("nested", "value")
                                    .build()))
                    .build()
                    .toString();
        }

        private DataAddress.Builder createDataAddress() {
            return DataAddress.Builder.newInstance().type("test-type")
                    .property(EDC_NAMESPACE + "complex", Map.of(
                            EDC_NAMESPACE + "simple", "value",
                            EDC_NAMESPACE + "nested",
                            Map.of(EDC_NAMESPACE + "innerValue", "value")));
        }

        private Asset.Builder createAsset() {
            return Asset.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .description("test description")
                    .dataAddress(createDataAddress().build())
                    .participantContextId(PARTICIPANT_CONTEXT_ID);
        }

        private JsonObjectBuilder createPropertiesBuilder() {
            return createObjectBuilder()
                    .add("name", "test-asset")
                    .add("description", "test description")
                    .add("version", "0.4.2")
                    .add("contentType", "application/json");
        }

        private JsonArray jsonLdContext() {
            return createArrayBuilder()
                    .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                    .build();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(1)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @RegisterExtension
        @Order(0)
        static final PostgresqlEndToEndExtension POSTGRES_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback SETUP = context -> {
            POSTGRES_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());
        };


        @Order(2)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();

    }

}
