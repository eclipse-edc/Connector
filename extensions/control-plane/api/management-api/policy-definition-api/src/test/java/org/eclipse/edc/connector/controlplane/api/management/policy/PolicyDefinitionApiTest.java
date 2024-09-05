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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.policy.transform.JsonObjectToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.api.management.policy.validation.PolicyDefinitionValidator;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_ERRORS;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_IS_VALID;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_TYPE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.v2.PolicyDefinitionApiV2.PolicyDefinitionInputSchema.POLICY_DEFINITION_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.v2.PolicyDefinitionApiV2.PolicyDefinitionOutputSchema.POLICY_DEFINITION_OUTPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.v31alpha.PolicyDefinitionApiV31Alpha.PolicyEvaluationPlanRequestSchema.POLICY_EVALUATION_PLAN_REQUEST_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.v31alpha.PolicyDefinitionApiV31Alpha.PolicyEvaluationPlanSchema.POLICY_EVALUATION_PLANE_OUTPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.v31alpha.PolicyDefinitionApiV31Alpha.PolicyValidationResultSchema.POLICY_VALIDATION_RESULT_OUTPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_CREATED_AT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_OBLIGATION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_POST_VALIDATORS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PRE_VALIDATORS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PROHIBITION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_TYPE;
import static org.mockito.Mockito.mock;

class PolicyDefinitionApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToPolicyDefinitionTransformer());
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(mock()).forEach(transformer::register);
    }

    @Test
    void policyDefinitionInputExample() throws JsonProcessingException {
        var validator = PolicyDefinitionValidator.instance();

        var jsonObject = objectMapper.readValue(POLICY_DEFINITION_INPUT_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, PolicyDefinition.class).getContent())
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getId()).isNotBlank();
                    assertThat(transformed.getPolicy()).isNotNull().satisfies(policy -> {
                        assertThat(policy.getPermissions()).asList().isNotEmpty();
                    });
                });
    }

    @Test
    void policyDefinitionOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(POLICY_DEFINITION_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(EDC_CREATED_AT).getJsonObject(0).getJsonNumber(VALUE).longValue()).isGreaterThan(0);
            assertThat(content.getJsonArray(EDC_POLICY_DEFINITION_POLICY).getJsonObject(0).size()).isGreaterThan(0);
        });
    }

    @Test
    void policyValidationResultExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(POLICY_VALIDATION_RESULT_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(EDC_POLICY_VALIDATION_RESULT_TYPE);
            assertThat(content.getJsonArray(EDC_POLICY_VALIDATION_RESULT_IS_VALID).getJsonObject(0).getBoolean(VALUE)).isEqualTo(false);
            assertThat(content.getJsonArray(EDC_POLICY_VALIDATION_RESULT_ERRORS).size()).isEqualTo(2);
        });
    }
    
    @Test
    void policyEvaluationPlanRequestExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(POLICY_EVALUATION_PLAN_REQUEST_INPUT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE);
            assertThat(content.getJsonArray(EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE).getJsonObject(0).getString(VALUE)).isEqualTo("catalog");
        });
    }

    @Test
    void policyEvaluationPlanOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(POLICY_EVALUATION_PLANE_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(EDC_POLICY_EVALUATION_PLAN_TYPE);
            assertThat(content.getJsonArray(EDC_POLICY_EVALUATION_PLAN_PRE_VALIDATORS)).hasSize(1);
            assertThat(content.getJsonArray(EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS)).hasSize(1);
            assertThat(content.getJsonArray(EDC_POLICY_EVALUATION_PLAN_PROHIBITION_STEPS)).hasSize(0);
            assertThat(content.getJsonArray(EDC_POLICY_EVALUATION_PLAN_OBLIGATION_STEPS)).hasSize(0);
            assertThat(content.getJsonArray(EDC_POLICY_EVALUATION_PLAN_POST_VALIDATORS)).hasSize(1);
        });
    }
}
