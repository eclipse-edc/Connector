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

package org.eclipse.edc.connector.api.management.contractdefinition.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
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

    private final JsonObjectFromContractDefinitionTransformer transformer = new JsonObjectFromContractDefinitionTransformer(jsonFactory);

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

}
