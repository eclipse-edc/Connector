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

package org.eclipse.edc.connector.api.management.contractdefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractdefinition.transform.JsonObjectToContractDefinitionRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.contractdefinition.validation.ContractDefinitionRequestDtoValidator;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.api.management.contractdefinition.ContractDefinitionApi.ContractDefinitionInputSchema.CONTRACT_DEFINITION_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.api.management.contractdefinition.ContractDefinitionApi.ContractDefinitionOutputSchema.CONTRACT_DEFINITION_OUTPUT_EXAMPLE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class ContractDefinitionApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());

    @Test
    void contractDefinitionInputExample() throws JsonProcessingException {
        var transformer = new JsonObjectToContractDefinitionRequestDtoTransformer();
        var validator = ContractDefinitionRequestDtoValidator.instance();

        var jsonObject = objectMapper.readValue(CONTRACT_DEFINITION_INPUT_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, mock()))
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getId()).isNotBlank();
                    assertThat(transformed.getAccessPolicyId()).isNotBlank();
                    assertThat(transformed.getContractPolicyId()).isNotBlank();
                    assertThat(transformed.getAssetsSelector()).asList().isEmpty();
                });
    }

    @Test
    void contractDefinitionOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(CONTRACT_DEFINITION_OUTPUT_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        assertThat(jsonObject.getString(ID)).isNotBlank();
        assertThat(jsonObject.getString("accessPolicyId")).isNotBlank();
        assertThat(jsonObject.getString("contractPolicyId")).isNotBlank();
        assertThat(jsonObject.getJsonArray("assetsSelector")).asList().isEmpty();
    }

}
