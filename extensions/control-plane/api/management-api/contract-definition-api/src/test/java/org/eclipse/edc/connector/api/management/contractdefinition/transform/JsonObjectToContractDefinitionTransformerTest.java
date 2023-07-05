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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractDefinitionTransformerTest {

    private final JsonObjectToContractDefinitionTransformer transformer = new JsonObjectToContractDefinitionTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(ContractDefinition.class);
    }

    @Test
    void transform() {
        var assetsSelectorJson = createArrayBuilder()
                .add(createArrayBuilder().build())
                .build();
        var criterion = Criterion.Builder.newInstance().operandLeft("any").operator("any").build();
        when(context.transform(any(), eq(Criterion.class))).thenReturn(criterion);
        var json = createObjectBuilder()
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "accessPolicyId")
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, "contractPolicyId")
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, assetsSelectorJson)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getAccessPolicyId()).isEqualTo("accessPolicyId");
        assertThat(result.getContractPolicyId()).isEqualTo("contractPolicyId");
        assertThat(result.getAssetsSelector()).containsExactly(criterion);
        verify(context).transform(any(), eq(Criterion.class));
    }

    @Test
    void transform_whenNoAssetSelectorItShouldBeAnEmptyList() {
        var json = createObjectBuilder()
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "accessPolicyId")
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, "contractPolicyId")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getAssetsSelector()).isEmpty();
        verify(context, never()).transform(any(), any());
    }

}
