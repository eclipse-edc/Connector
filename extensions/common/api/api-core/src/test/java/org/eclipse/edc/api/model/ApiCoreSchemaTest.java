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

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.transformer.JsonObjectToCriterionDtoTransformer;
import org.eclipse.edc.api.transformer.JsonObjectToQuerySpecDtoTransformer;
import org.eclipse.edc.api.validation.CriterionDtoValidator;
import org.eclipse.edc.api.validation.QuerySpecDtoValidator;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.api.model.ApiCoreSchema.CriterionSchema.CRITERION_EXAMPLE;
import static org.eclipse.edc.api.model.ApiCoreSchema.IdResponseSchema.ID_RESPONSE_EXAMPLE;
import static org.eclipse.edc.api.model.ApiCoreSchema.QuerySpecSchema.QUERY_SPEC_EXAMPLE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class ApiCoreSchemaTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());

    @Test
    void criterionExample() throws JsonProcessingException {
        var transformer = new JsonObjectToCriterionDtoTransformer();
        var validator = CriterionDtoValidator.instance();

        var jsonObject = objectMapper.readValue(CRITERION_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, mock()))
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getOperandLeft()).isNotNull();
                    assertThat(transformed.getOperator()).isNotBlank();
                    assertThat(transformed.getOperandRight()).isNotNull();
                });
    }

    @Test
    void querySpecExample() throws JsonProcessingException {
        var transformer = new JsonObjectToQuerySpecDtoTransformer();
        var validator = QuerySpecDtoValidator.instance();

        var jsonObject = objectMapper.readValue(QUERY_SPEC_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, mock()))
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getOffset()).isGreaterThan(0);
                    assertThat(transformed.getLimit()).isGreaterThan(1);
                    assertThat(transformed.getSortOrder()).isNotNull();
                    assertThat(transformed.getSortField()).isNotBlank();
                });
    }

    @Test
    void idResponseExample() throws JsonProcessingException {
        var idResponse = objectMapper.readValue(ID_RESPONSE_EXAMPLE, JsonObject.class);

        assertThat(idResponse).isNotNull();
        assertThat(idResponse.getString(ID)).isNotBlank();
        assertThat(idResponse.getJsonNumber("createdAt").longValue()).isGreaterThan(0);
    }
}
