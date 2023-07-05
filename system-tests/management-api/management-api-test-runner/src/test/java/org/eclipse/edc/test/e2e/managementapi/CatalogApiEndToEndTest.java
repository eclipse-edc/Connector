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

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class CatalogApiEndToEndTest extends BaseManagementApiEndToEndTest {

    // requests the catalog to itself, to save another connector.
    private final String providerUrl = "http://localhost:" + PROTOCOL_PORT + "/protocol";

    @Test
    void shouldReturnCatalog_withoutQuerySpec() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "CatalogRequest")
                .add("providerUrl", providerUrl)
                .add("protocol", "dataspace-protocol-http")
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
        var asset = createAsset("id-1").dataAddress(createDataAddress().build());
        var asset1 = createAsset("id-2").dataAddress(createDataAddress().build());

        var assetIndex = controlPlane.getContext().getService(AssetIndex.class);
        var policyDefinitionStore = controlPlane.getContext().getService(PolicyDefinitionStore.class);
        var contractDefinitionStore = controlPlane.getContext().getService(ContractDefinitionStore.class);

        var policyId = UUID.randomUUID().toString();

        var cd = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractPolicyId(policyId)
                .accessPolicyId(policyId)
                .build();

        var policy = Policy.Builder.newInstance()
                .build();

        policyDefinitionStore.create(PolicyDefinition.Builder.newInstance().id(policyId).policy(policy).build());
        contractDefinitionStore.save(cd);

        assetIndex.create(asset.build());
        assetIndex.create(asset1.build());

        var criteria = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(TYPE, "CriterionDto")
                        .add("operandLeft", EDC_NAMESPACE + "id")
                        .add("operator", "=")
                        .add("operandRight", "id-2")
                        .build()
                )
                .build();

        var querySpec = createObjectBuilder()
                .add(TYPE, "QuerySpecDto")
                .add("filterExpression", criteria)
                .add("limit", 1);

        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "CatalogRequest")
                .add("providerUrl", providerUrl)
                .add("protocol", "dataspace-protocol-http")
                .add("querySpec", querySpec)
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
