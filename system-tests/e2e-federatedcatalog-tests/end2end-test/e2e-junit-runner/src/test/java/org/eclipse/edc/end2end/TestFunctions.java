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

package org.eclipse.edc.end2end;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.query.QuerySpec;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_DATA_ADDRESS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION;

public class TestFunctions {

    public static String createPolicy(String policyId, String assetId) {
        return getResourceFileContentAsString("policy.json")
                .replace("http://example.com/policy:1010", policyId)
                .replace("http://example.com/asset:9898.movie", assetId);
    }

    public static JsonObject createAssetJson(String assetId) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, assetId)
                .add(EDC_ASSET_PROPERTIES, createPropertiesBuilder(assetId).build())
                .add(EDC_ASSET_DATA_ADDRESS, createDataAddressJson())
                .build();
    }

    public static JsonObject createContractDef(String id, String accessPolicyId, String contractPolicyId, String assetId) {
        return Json.createObjectBuilder()
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(ID, id)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, accessPolicyId)
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, contractPolicyId)
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, createCriterionBuilder(Asset.PROPERTY_ID, "=", assetId).build())
                .build();
    }

    public static JsonObject createEmptyQuery() {
        return Json.createObjectBuilder()
                .add(TYPE, QuerySpec.EDC_QUERY_SPEC_TYPE)
                .build();
    }

    public static JsonObject createQuerySpecWithFilterExpressionForAssetId(String id) {
        return Json.createObjectBuilder()
                .add(TYPE, QuerySpec.EDC_QUERY_SPEC_TYPE)
                .add(EDC_QUERY_SPEC_FILTER_EXPRESSION, createCriterionBuilder("datasets.id", "=", id))
                .build();
    }


    private static JsonArrayBuilder createCriterionBuilder(String operandLeft, String operator, String operandRight) {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "Criterion")
                        .add(EDC_NAMESPACE + "operandLeft", operandLeft)
                        .add(EDC_NAMESPACE + "operator", operator)
                        .add(EDC_NAMESPACE + "operandRight", operandRight)
                );
    }

    private static JsonObjectBuilder createPropertiesBuilder(String id) {
        return Json.createObjectBuilder()
                .add(Asset.PROPERTY_NAME, "test-asset-" + id)
                .add(Asset.PROPERTY_ID, id);
    }

    private static JsonObjectBuilder createContextBuilder() {
        return Json.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private static JsonObject createDataAddressJson() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "test-src-type")
                .build();
    }
}
