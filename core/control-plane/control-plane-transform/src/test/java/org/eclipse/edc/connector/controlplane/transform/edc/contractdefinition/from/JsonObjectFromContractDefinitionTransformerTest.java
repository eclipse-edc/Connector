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
 *       SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromContractDefinitionTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private final TypeManager typeManager = mock();
    private final JsonObjectFromContractDefinitionTransformer transformer = new JsonObjectFromContractDefinitionTransformer(jsonFactory, typeManager, "test");

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var criterionJson = jsonFactory.createObjectBuilder().build();
        when(context.transform(isA(Criterion.class), eq(JsonObject.class))).thenReturn(criterionJson);
        var criterion = criterion("left", "=", "right");
        var definition = ContractDefinition.Builder.newInstance()
                .id("id")
                .accessPolicyId("accessPolicyId")
                .contractPolicyId("contractPolicyId")
                .assetsSelector(List.of(criterion))
                .build();

        var result = transformer.transform(definition, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("id");
        assertThat(result.getString(TYPE)).isEqualTo(CONTRACT_DEFINITION_TYPE);
        assertThat(result.getString(CONTRACT_DEFINITION_ACCESSPOLICY_ID)).isEqualTo("accessPolicyId");
        assertThat(result.getString(CONTRACT_DEFINITION_CONTRACTPOLICY_ID)).isEqualTo("contractPolicyId");
        assertThat(result.getJsonArray(CONTRACT_DEFINITION_ASSETS_SELECTOR)).containsExactly(criterionJson);
        verify(context).transform(criterion, JsonObject.class);
        verify(context, never()).reportProblem(anyString());
    }


    @Test
    void transform_withPrivateProperties_simpleTypes() {
        var criterionJson = jsonFactory.createObjectBuilder().build();
        when(context.transform(isA(Criterion.class), eq(JsonObject.class))).thenReturn(criterionJson);
        var criterion = criterion("left", "=", "right");
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("id")
                .accessPolicyId("accessPolicyId")
                .contractPolicyId("contractPolicyId")
                .assetsSelector(List.of(criterion))
                .privateProperty("some-key", "some-value")
                .build();

        var jsonObject = transformer.transform(contractDefinition, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonObject(CONTRACT_DEFINITION_PRIVATE_PROPERTIES).getJsonString("some-key").getString()).isEqualTo("some-value");
    }

}
