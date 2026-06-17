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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.validation.ContractDefinitionValidator;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.to.JsonObjectToContractDefinitionTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v3.ContractDefinitionApiV3.ContractDefinitionInputSchema.CONTRACT_DEFINITION_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v3.ContractDefinitionApiV3.ContractDefinitionOutputSchema.CONTRACT_DEFINITION_OUTPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.test.TestJsonLd.expand;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class ContractDefinitionApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToContractDefinitionTransformer());
    }

    @Test
    void contractDefinitionInputExample() throws JsonProcessingException {
        var validator = ContractDefinitionValidator.instance(mock());

        var jsonObject = objectMapper.readValue(CONTRACT_DEFINITION_INPUT_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = expand(jsonObject);
        assertThat(validator.validate(expanded)).isSucceeded();
        var transformed = transformer.transform(expanded, ContractDefinition.class).getContent();
        assertThat(transformed).isNotNull()
                .satisfies(t -> {
                    assertThat(t.getId()).isNotBlank();
                    assertThat(t.getAccessPolicyId()).isNotBlank();
                    assertThat(t.getContractPolicyId()).isNotBlank();
                    assertThat(t.getAssetsSelector()).asList().isEmpty();
                });
    }

    @Test
    void contractDefinitionOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(CONTRACT_DEFINITION_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = expand(jsonObject);

        assertThat(expanded.getString(ID)).isNotBlank();
        assertThat(expanded.getJsonArray(CONTRACT_DEFINITION_ACCESSPOLICY_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
        assertThat(expanded.getJsonArray(CONTRACT_DEFINITION_CONTRACTPOLICY_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
        assertThat(expanded.getJsonArray(CONTRACT_DEFINITION_ASSETS_SELECTOR)).asList().isEmpty();
    }

}
