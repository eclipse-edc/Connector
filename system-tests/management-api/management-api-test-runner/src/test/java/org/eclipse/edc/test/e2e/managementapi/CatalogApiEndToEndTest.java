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

import jakarta.json.Json;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERATOR;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_TYPE;
import static org.eclipse.edc.api.query.QuerySpecDto.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.api.query.QuerySpecDto.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.api.query.QuerySpecDto.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class CatalogApiEndToEndTest extends BaseManagementApiEndToEndTest {

    private static final String TEST_ASSET_ID = "test-asset-id";

    // requests the catalog to itself, to save another connector.
    private final String providerUrl = "http://localhost:" + PROTOCOL_PORT + "/protocol";

    @Test
    void shouldReturnCatalog_withoutQuerySpec() {

        var requestBody = Json.createObjectBuilder()
                .add(TYPE, EDC_CATALOG_REQUEST_TYPE)
                .add(EDC_CATALOG_REQUEST_PROVIDER_URL, providerUrl)
                .add(EDC_CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http")
                .build();

        given()
                .port(PORT)
                .contentType(JSON)
                .body(requestBody)
                .basePath("/management/v2/catalog")
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is("dcat:Catalog"));
    }

    @Test
    void shouldReturnCatalog_withQuerySpec() {

        var asset = createAsset("id-1");
        var asset1 = createAsset("id-2");

        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);
        var policyDefinitionStore = controlPlane.getContext().getService(PolicyDefinitionStore.class);
        var contractDefinitionStore = controlPlane.getContext().getService(ContractDefinitionStore.class);

        var policyId = UUID.randomUUID().toString();

        var cd = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractPolicyId(policyId)
                .accessPolicyId(policyId)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        var policy = Policy.Builder.newInstance()
                .build();

        policyDefinitionStore.create(PolicyDefinition.Builder.newInstance().id(policyId).policy(policy).build());
        contractDefinitionStore.save(cd);

        assetIndex.create(new AssetEntry(asset.build(), createDataAddress().build()));
        assetIndex.create(new AssetEntry(asset1.build(), createDataAddress().build()));

        var criteria = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(TYPE, CRITERION_TYPE)
                        .add(CRITERION_OPERAND_LEFT, EDC_NAMESPACE + "id")
                        .add(CRITERION_OPERATOR, "=")
                        .add(CRITERION_OPERAND_RIGHT, "id-2")
                        .build()
                )
                .build();

        var querySpec = Json.createObjectBuilder()
                .add(TYPE, EDC_QUERY_SPEC_TYPE)
                .add(EDC_QUERY_SPEC_FILTER_EXPRESSION, criteria)
                .add(EDC_QUERY_SPEC_LIMIT, 1)
                .build();

        var requestBody = Json.createObjectBuilder()
                .add(TYPE, EDC_CATALOG_REQUEST_TYPE)
                .add(EDC_CATALOG_REQUEST_PROVIDER_URL, providerUrl)
                .add(EDC_CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http")
                .add(EDC_CATALOG_REQUEST_QUERY_SPEC, querySpec)
                .build();

        given()
                .port(PORT)
                .contentType(JSON)
                .body(requestBody)
                .basePath("/management/v2/catalog")
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is("dcat:Catalog"))
                .body("'dcat:dataset'.'edc:id'", is("id-2"))
                .extract().body().asString();
    }

    private DataAddress.Builder createDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type");
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id);
    }

}
