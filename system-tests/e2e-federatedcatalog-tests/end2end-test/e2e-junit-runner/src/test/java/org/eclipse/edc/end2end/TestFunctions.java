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

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;

public class TestFunctions {

    public static String createPolicy(String policyId, String assetId) {
        return getResourceFileContentAsString("policy.json")
                .replace("http://example.com/policy:1010", policyId)
                .replace("http://example.com/asset:9898.movie", assetId);
    }

    public static JsonObject createAssetJson(String assetId) {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                        .build())
                .add(TYPE, EDC_ASSET_TYPE_TERM)
                .add(ID, assetId)
                .add("properties", createPropertiesBuilder(assetId).build())
                .add("dataAddress", createDataAddressJson())
                .build();
    }

    public static JsonObject createContractDef(String id, String accessPolicyId, String contractPolicyId, String assetId) {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                        .build())
                .add(TYPE, CONTRACT_DEFINITION_TYPE_TERM)
                .add(ID, id)
                .add("accessPolicyId", accessPolicyId)
                .add("contractPolicyId", contractPolicyId)
                .add("assetsSelector", createCriterionBuilder(Asset.PROPERTY_ID, "=", assetId).build())
                .build();
    }


    public static JsonArrayBuilder createCriterionBuilder(String operandLeft, String operator, String operandRight) {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(TYPE, "Criterion")
                        .add("operandLeft", operandLeft)
                        .add("operator", operator)
                        .add("operandRight", operandRight)
                );
    }

    private static JsonObjectBuilder createPropertiesBuilder(String id) {
        return Json.createObjectBuilder()
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
