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

package org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractDefinitionTransformerTest {

    private final JsonObjectToContractDefinitionTransformer transformer = new JsonObjectToContractDefinitionTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));

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


    @Test
    void transform_withPrivateProperties() {
        when(context.transform(any(), eq(Object.class))).thenReturn("test-val");
        var jsonObj = createObjectBuilder()
                .add(CONTEXT, createContextBuilder().addNull(EDC_PREFIX).build())
                .add(ID, "some-contract-definition-id")
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "accessPolicyId")
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, "contractPolicyId")
                .add(CONTRACT_DEFINITION_PRIVATE_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add("test-prop", "test-val").build()).build())
                .build();

        jsonObj = expand(jsonObj);
        var contractDefinition = transformer.transform(jsonObj, context);

        assertThat(contractDefinition.getPrivateProperties())
                .hasSize(1)
                .containsEntry(EDC_NAMESPACE + "test-prop", "test-val");
    }

    private JsonObject expand(JsonObject jsonObject) {
        return jsonLd.expand(jsonObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }

    private JsonObjectBuilder createContextBuilder() {
        return createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

}
