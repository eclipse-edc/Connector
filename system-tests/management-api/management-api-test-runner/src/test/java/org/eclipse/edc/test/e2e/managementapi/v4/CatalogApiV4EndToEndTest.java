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

package org.eclipse.edc.test.e2e.managementapi.v4;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndTestContext;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class CatalogApiV4EndToEndTest {

    public static final String PARTICIPANT_CONTEXT_ID = "anonymous";

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @Test
        void requestCatalog_shouldReturnCatalog_withoutQuerySpec(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerDsp2025url())
                    .add("counterPartyId", "counter-party-id")
                    .add("protocol", "dataspace-protocol-http:2025-1")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/catalog/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"));
        }

        @Test
        void requestCatalog_shouldReturnCatalog_withQuerySpec(ManagementEndToEndTestContext context, AssetIndex assetIndex,
                                                              PolicyDefinitionStore policyDefinitionStore,
                                                              ContractDefinitionStore contractDefinitionStore) {

            assetIndex.create(createAsset("id-1", "test-type").build());
            assetIndex.create(createAsset("id-2", "test-type").build());
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
                    .add("counterPartyAddress", context.providerDsp2025url())
                    .add("counterPartyId", "counter-party-id")
                    .add("protocol", "dataspace-protocol-http:2025-1")
                    .add("querySpec", querySpec)
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/catalog/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"))
                    .body("dataset[0].id", is("id-2"));
        }

        @Test
        void requestCatalog_shouldReturnBadRequest_withMissingFields(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/catalog/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .contentType(JSON)
                    .body("[0].message", containsString("required property 'protocol' not found"))
                    .body("[1].message", containsString("required property 'counterPartyAddress' not found"))
                    .body("[2].message", containsString("required property 'counterPartyId' not found"));
        }

        @Test
        void requestCatalog_whenAssetIsCatalogAsset_shouldReturnCatalogOfCatalogs(ManagementEndToEndTestContext context, AssetIndex assetIndex,
                                                                                  PolicyDefinitionStore policyDefinitionStore,
                                                                                  ContractDefinitionStore contractDefinitionStore) {

            // create CatalogAsset
            var catalogAssetId = "catalog-asset-" + UUID.randomUUID();
            var httpData = createAsset(catalogAssetId, "HttpData")
                    .property(Asset.PROPERTY_IS_CATALOG, true)
                    .build();
            httpData.getDataAddress().getProperties().put(EDC_NAMESPACE + "baseUrl", "http://quizzqua.zz/buzz");
            assetIndex.create(httpData);

            // create conventional asset
            var normalAssetId = "normal-asset-" + UUID.randomUUID();
            assetIndex.create(createAsset(normalAssetId, "test-type").build());

            var assetSelectorCriteria = List.of(Criterion.criterion("id", "in", List.of(catalogAssetId, normalAssetId)));

            createContractOffer(policyDefinitionStore, contractDefinitionStore, assetSelectorCriteria);


            // request all assets
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerDsp2025url())
                    .add("counterPartyId", "counter-party-id")
                    .add("protocol", "dataspace-protocol-http:2025-1")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/catalog/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("Catalog"))
                    .body("'service'", notNullValue())
                    // findAll is the restAssured way to express JSON Path filters
                    .body("catalog[0].'@type'", equalTo("Catalog"))
                    .body("catalog[0].isCatalog", equalTo(true))
                    .body("catalog[0].'@id'", equalTo(catalogAssetId))
                    .body("catalog[0].service[0].endpointURL", equalTo("http://quizzqua.zz/buzz"))
                    .body("catalog[0].distribution[0].accessService.'@id'", equalTo(Base64.getUrlEncoder().encodeToString(catalogAssetId.getBytes())));
        }

        @Test
        void getDataset_shouldReturnDataset(ManagementEndToEndTestContext context, AssetIndex assetIndex,
                                            PolicyDefinitionStore policyDefinitionStore,
                                            ContractDefinitionStore contractDefinitionStore,
                                            DataPlaneInstanceStore dataPlaneInstanceStore) {
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedSourceType("test-type").allowedTransferType("any-PULL").build();
            dataPlaneInstanceStore.save(dataPlaneInstance);

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());
            assetIndex.create(createAsset("asset-id", "test-type").build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", context.providerDsp2025url())
                    .add("counterPartyId", "counter-party-id")
                    .add("protocol", "dataspace-protocol-http:2025-1")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/catalog/dataset/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-id"))
                    .body(TYPE, is("Dataset"))
                    .body("distribution[0].accessService.'@id'", notNullValue());
        }

        @Test
        void getDatasetWithResponseChannel_shouldReturnDataset(ManagementEndToEndTestContext context, AssetIndex assetIndex,
                                                               DataPlaneInstanceStore dataPlaneInstanceStore,
                                                               PolicyDefinitionStore policyDefinitionStore,
                                                               ContractDefinitionStore contractDefinitionStore) {

            createContractOffer(policyDefinitionStore, contractDefinitionStore, List.of());

            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedDestType("any").allowedSourceType("test-type")
                    .allowedTransferType("any-PULL").allowedTransferType("any-PULL-response").build();
            dataPlaneInstanceStore.save(dataPlaneInstance);

            var responseChannel = DataAddress.Builder.newInstance()
                    .type("response")
                    .build();

            var dataAddressWithResponseChannel = DataAddress.Builder.newInstance()
                    .type("test-type")
                    .responseChannel(responseChannel)
                    .build();
            assetIndex.create(createAsset("asset-response", dataAddressWithResponseChannel).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-response")
                    .add("counterPartyAddress", context.providerDsp2025url())
                    .add("counterPartyId", "counter-party-id")
                    .add("protocol", "dataspace-protocol-http:2025-1")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/catalog/dataset/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-response"))
                    .body(TYPE, is("Dataset"))
                    .body("distribution[0].format", is("any-PULL-response"));
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
            return createAsset(id, address);
        }

        private Asset.Builder createAsset(String id, DataAddress address) {
            return Asset.Builder.newInstance()
                    .dataAddress(address)
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .id(id);
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(postgres::config)
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();

    }

}
