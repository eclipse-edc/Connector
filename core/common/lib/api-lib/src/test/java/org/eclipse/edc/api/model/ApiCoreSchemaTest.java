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
import org.eclipse.edc.api.validation.DataAddressValidator;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.validator.jsonobject.validators.model.CriterionValidator;
import org.eclipse.edc.validator.jsonobject.validators.model.QuerySpecValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.edc.api.model.ApiCoreSchema.CriterionSchema.CRITERION_EXAMPLE;
import static org.eclipse.edc.api.model.ApiCoreSchema.DataAddressSchema.DATA_ADDRESS_EXAMPLE;
import static org.eclipse.edc.api.model.ApiCoreSchema.IdResponseSchema.ID_RESPONSE_EXAMPLE;
import static org.eclipse.edc.api.model.ApiCoreSchema.QuerySpecSchema.QUERY_SPEC_EXAMPLE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiCoreSchemaTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();
    private final CriterionOperatorRegistry criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToQuerySpecTransformer());
        transformer.register(new JsonObjectToCriterionTransformer());
        transformer.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        transformer.register(new JsonObjectToDataAddressTransformer());
        when(typeManager.getMapper("test")).thenReturn(objectMapper);
    }

    @Test
    void criterionExample() throws JsonProcessingException {
        var validator = CriterionValidator.instance(criterionOperatorRegistry);

        var jsonObject = objectMapper.readValue(CRITERION_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, Criterion.class).getContent())
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getOperandLeft()).isNotNull();
                    assertThat(transformed.getOperator()).isNotBlank();
                    assertThat(transformed.getOperandRight()).isNotNull();
                });
    }

    @Test
    void querySpecExample() throws JsonProcessingException {
        var validator = QuerySpecValidator.instance(criterionOperatorRegistry);

        var jsonObject = objectMapper.readValue(QUERY_SPEC_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, QuerySpec.class).getContent())
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

    @Test
    void apiErrorDetailExample() throws JsonProcessingException {
        var apiErrorDetail = objectMapper.readValue(ApiCoreSchema.ApiErrorDetailSchema.API_ERROR_EXAMPLE, JsonObject.class);

        assertThat(apiErrorDetail.getString("message")).isNotBlank();
        assertThat(apiErrorDetail.getString("type")).isNotBlank();
        assertThat(apiErrorDetail.getString("path")).isNotBlank();
        assertThat(apiErrorDetail.getString("invalidValue")).isNotBlank();
    }

    @Test
    void dataAddressExample() throws JsonProcessingException {
        var validator = DataAddressValidator.instance();

        var jsonObject = objectMapper.readValue(DATA_ADDRESS_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, DataAddress.class).getContent())
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getType()).isNotBlank();
                    assertThat(transformed.getProperties()).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
                });
    }

}
