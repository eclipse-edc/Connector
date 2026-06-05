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

import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.jsonld.spi.PropertyAndTypeNames;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
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

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_CONFORMS_TO_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;

public class CatalogApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {
        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
        /**
         * This means that all Catalog requests will ultimately loop back to this runtime's own DSP API
         */
        public static final String COUNTER_PARTY_ID = PARTICIPANT_CONTEXT_ID;

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService, ParticipantContextConfigStore configStore) {

            createParticipant(participantContextService, configStore, PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, DataPlaneInstanceStore dataPlaneInstanceStore) {
            var list = participantContextService.search(QuerySpec.max())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            for (var p : list) {
                participantContextService.deleteParticipantContext(p.getParticipantContextId()).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            }

            dataPlaneInstanceStore.getAll().toList().forEach(dp -> dataPlaneInstanceStore.deleteById(dp.getId()));
        }

        @Test
        void requestCatalog_tokenBearerNotOwner(ManagementEndToEndV5TestContext context,
                                                OauthServer authServer,
                                                ParticipantContextService srv, ParticipantContextConfigStore store) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            var otherParticipant = "other-participant";
            createParticipant(srv, store, otherParticipant);
            var token = authServer.createToken(otherParticipant);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource".formatted(otherParticipant)));
        }

        @Test
        void requestCatalog_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(authServer.createAdminToken())
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"));
        }

        @Test
        void requestCatalog_tokenLacksScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:fizzbuzz"));

            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:read.*missing.*"));
        }

        @Test
        void requestCatalog_tokenHasWrongRole(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("role", "some-role"));

            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied"));
        }

        @Test
        void requestCatalog_shouldReturnCatalog_withoutQuerySpec(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"));
        }

        @Test
        void requestCatalog_shouldReturnCatalog_withQuerySpec(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                              PolicyDefinitionStore policyDefinitionStore,
                                                              ContractDefinitionStore contractDefinitionStore) {

            assetIndex.create(createAsset("id-1", "test-type").build());

            var asset2 = createAsset("id-2", "test-type")
                    .property(DCT_CONFORMS_TO_ATTRIBUTE, Map.of(ID, "https://example.org/schema")).build();

            assetIndex.create(asset2);
            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());

            var criteria = createArrayBuilder()
                    .add(createObjectBuilder()
                            .add(TYPE, "Criterion")
                            .add("operandLeft", EDC_NAMESPACE + "id")
                            .add("operator", "=")
                            .add("operandRight", "id-2")
                            .build()
                    )
                    .build();

            var querySpec = createObjectBuilder()
                    .add(TYPE, "QuerySpec")
                    .add("filterExpression", criteria)
                    .add("limit", 1);

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .add("querySpec", querySpec)
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"))
                    .body("dataset[0].id", is("id-2"))
                    .body("dataset[0].conformsTo", is("https://example.org/schema"));
        }

        @Test
        void requestCatalog_shouldReturnBadRequest_withMissingFields(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .contentType(JSON)
                    .body("[1].message", containsString("required property 'protocol' not found"))
                    .body("[2].message", containsString("required property 'profile' not found"))
                    .body("[3].message", containsString("required property 'counterPartyAddress' not found"))
                    .body("[4].message", containsString("required property 'counterPartyId' not found"));
        }

        @Test
        void requestCatalog_shouldReturnBadRequest_withWrongProfile(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", "wrong-profile")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .contentType(JSON)
                    .body("[0].message", containsString("No profile 'wrong-profile' for participant 'test-participant'"));
        }

        @Test
        void requestCatalog_whenAssetIsCatalogAsset_shouldReturnCatalogOfCatalogs(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                                                  PolicyDefinitionStore policyDefinitionStore,
                                                                                  ContractDefinitionStore contractDefinitionStore) {

            var catalogAssetId = "catalog-asset-" + UUID.randomUUID();
            var catalogAsset = createAssetBuilder(catalogAssetId)
                    .property(Asset.PROPERTY_IS_CATALOG, true)
                    .property(PropertyAndTypeNames.DCAT_ENDPOINT_URL_ATTRIBUTE, "http://quizzqua.zz/buzz")
                    .property(PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE, "format")
                    .participantContextId(COUNTER_PARTY_ID)
                    .build();

            assetIndex.create(catalogAsset);

            var conventionalAssetId = "normal-asset-" + UUID.randomUUID();
            var conventionalAsset = createAsset(conventionalAssetId, "test-type").participantContextId(COUNTER_PARTY_ID).build();

            Stream.of(catalogAsset, conventionalAsset).forEach(assetIndex::create);

            var assetSelectorCriteria = List.of(Criterion.criterion("id", "in", List.of(catalogAssetId, conventionalAssetId)));
            createContractOffer(policyDefinitionStore, contractDefinitionStore, assetSelectorCriteria);

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"))
                    .body("'service'", notNullValue())
                    .body("catalog[0].'@type'", equalTo("Catalog"))
                    .body("catalog[0].isCatalog", equalTo(true))
                    .body("catalog[0].'@id'", equalTo(catalogAssetId))
                    .body("catalog[0].service[0].endpointURL", equalTo("http://quizzqua.zz/buzz"))
                    .body("catalog[0].distribution[0].accessService.'@id'", equalTo(Base64.getUrlEncoder().encodeToString(catalogAssetId.getBytes())))
                    .body("catalog[0].distribution[0].format", equalTo("format"));
        }

        @Test
        void getDataset_shouldReturnDataset(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                            PolicyDefinitionStore policyDefinitionStore,
                                            ContractDefinitionStore contractDefinitionStore,
                                            DataPlaneInstanceStore dataPlaneInstanceStore) {
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedSourceType("test-type").allowedTransferType("any-PULL")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            dataPlaneInstanceStore.save(dataPlaneInstance);

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());
            assetIndex.create(createAsset("asset-id", "test-type").build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-id"))
                    .body(TYPE, is("Dataset"))
                    .body("distribution[0].accessService.'@id'", notNullValue());
        }

        @Test
        void getDataset_shouldReturnBadRequest_whenWrongProfile(ManagementEndToEndV5TestContext context) {

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", "wrong-profile")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .contentType(JSON)
                    .body("[0].message", containsString("No profile 'wrong-profile' for participant 'test-participant'"));
        }

        @Test
        void getDatasetWithResponseChannel_shouldReturnDataset(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                               DataPlaneInstanceStore dataPlaneInstanceStore,
                                                               PolicyDefinitionStore policyDefinitionStore,
                                                               ContractDefinitionStore contractDefinitionStore) {

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());

            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedDestType("any").allowedSourceType("test-type")
                    .allowedTransferType("any-PULL").allowedTransferType("any-PULL-response")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            dataPlaneInstanceStore.save(dataPlaneInstance);

            var responseChannel = DataAddress.Builder.newInstance()
                    .type("response")
                    .build();

            var dataAddressWithResponseChannel = DataAddress.Builder.newInstance()
                    .type("test-type")
                    .responseChannel(responseChannel)
                    .build();
            assetIndex.create(createAssetBuilder("asset-response").dataAddress(dataAddressWithResponseChannel).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-response")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-response"))
                    .body(TYPE, is("Dataset"))
                    .body("distribution[0].format", is("any-PULL-response"));
        }

        @Test
        void getDataset_shouldFilterDistributionsByAssetProfiles(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                                 DataPlaneInstanceStore dataPlaneInstanceStore,
                                                                 PolicyDefinitionStore policyDefinitionStore,
                                                                 ContractDefinitionStore contractDefinitionStore) {
            var httpDataPlane = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedSourceType("test-type").allowedTransferType("Http-PULL")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            var s3DataPlane = DataPlaneInstance.Builder.newInstance().url("http://localhost/any-s3")
                    .allowedSourceType("test-type").allowedTransferType("S3-PULL")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            dataPlaneInstanceStore.save(httpDataPlane);
            dataPlaneInstanceStore.save(s3DataPlane);

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());
            var metadata = DataplaneMetadata.Builder.newInstance().profile("Http-PULL").build();
            assetIndex.create(createAsset("asset-with-profile", "test-type").dataplaneMetadata(metadata).build());

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-with-profile")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-with-profile"))
                    .body("distribution.size()", is(1))
                    .body("distribution[0].format", is("Http-PULL"));
        }

        @Test
        void getDataset_shouldReturnAllDistributions_whenAssetHasNoProfiles(ManagementEndToEndV5TestContext context, AssetIndex assetIndex,
                                                                            DataPlaneInstanceStore dataPlaneInstanceStore,
                                                                            PolicyDefinitionStore policyDefinitionStore,
                                                                            ContractDefinitionStore contractDefinitionStore) {
            var httpDataPlane = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedSourceType("test-type").allowedTransferType("Http-PULL")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            var s3DataPlane = DataPlaneInstance.Builder.newInstance().url("http://localhost/any-s3")
                    .allowedSourceType("test-type").allowedTransferType("S3-PULL")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            dataPlaneInstanceStore.save(httpDataPlane);
            dataPlaneInstanceStore.save(s3DataPlane);

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());
            assetIndex.create(createAsset("asset-no-profile", "test-type").build());

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-no-profile")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-no-profile"))
                    .body("distribution.size()", is(2));
        }

        @Test
        void getDataset_tokenBearerNotOwner(ManagementEndToEndV5TestContext context,
                                            OauthServer authServer,
                                            ParticipantContextService srv, ParticipantContextConfigStore store) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();


            var otherParticipant = "other-participant";
            createParticipant(srv, store, otherParticipant);
            var token = authServer.createToken(otherParticipant);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource".formatted(otherParticipant)));
        }

        @Test
        void getDataset_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, DataPlaneInstanceStore dataPlaneInstanceStore,
                                           PolicyDefinitionStore policyDefinitionStore, ContractDefinitionStore contractDefinitionStore,
                                           AssetIndex assetIndex, OauthServer authServer) {
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedSourceType("test-type").allowedTransferType("any-PULL")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();
            dataPlaneInstanceStore.save(dataPlaneInstance);

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());
            assetIndex.create(createAsset("asset-id", "test-type").build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            context.baseRequest(authServer.createAdminToken())
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-id"))
                    .body(TYPE, is("Dataset"))
                    .body("distribution[0].accessService.'@id'", notNullValue());
        }

        @Test
        void getDataset_tokenLacksScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("protocol", context.profile())
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:fizzbuzz"));

            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:read.*missing.*"));
        }

        @Test
        void getDataset_tokenHasWrongRole(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerProtocolUrl(COUNTER_PARTY_ID, context.profile()))
                    .add("counterPartyId", COUNTER_PARTY_ID)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("role", "some-role"));

            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/dataset/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied"));
        }

        private void createContractOffer(PolicyDefinitionStore policyStore, ContractDefinitionStore contractDefStore, List<Criterion> assetsSelectorCritera) {

            var policyId = UUID.randomUUID().toString();

            var policy = Policy.Builder.newInstance()
                    .build();

            var contractDefinition = ContractDefinition.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .contractPolicyId(policyId)
                    .accessPolicyId(policyId)
                    .assetsSelector(assetsSelectorCritera)
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .build();


            policyStore.create(PolicyDefinition.Builder.newInstance().id(policyId)
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .policy(policy).build());
            contractDefStore.save(contractDefinition);

        }

        private Asset.Builder createAsset(String id, String sourceType) {
            var address = DataAddress.Builder.newInstance()
                    .type(sourceType)
                    .build();

            return createAssetBuilder(id)
                    .dataAddress(address);
        }

        private Asset.Builder createAssetBuilder(String id) {
            return Asset.Builder.newInstance()
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .id(id);
        }

        private void createParticipant(ParticipantContextService participantContextService,
                                       ParticipantContextConfigStore configStore, String participantContextId) {
            var pc = ParticipantContext.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .state(ParticipantContextState.ACTIVATED)
                    .identity(participantContextId)
                    .build();

            var config = ParticipantContextConfiguration.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .entries(Map.of("edc.mock.region", "eu",
                            "edc.participant.id", "did:web:" + participantContextId
                    ))
                    .build();

            configStore.save(config);

            participantContextService.createParticipantContext(pc)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
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
                .paramProvider(ManagementEndToEndV5TestContext.class,
                        ManagementEndToEndV5TestContext::forContext)
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
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION
                        .configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();

    }

}