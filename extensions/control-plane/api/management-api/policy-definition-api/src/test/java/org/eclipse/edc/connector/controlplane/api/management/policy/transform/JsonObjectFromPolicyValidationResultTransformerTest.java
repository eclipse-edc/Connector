/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.policy.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_ERRORS;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_IS_VALID;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromPolicyValidationResultTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());

    private final JsonObjectFromPolicyValidationResultTransformer transformer = new JsonObjectFromPolicyValidationResultTransformer(jsonFactory);
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getInputType()).isEqualTo(PolicyValidationResult.class);
    }

    @Test
    void transform() {
        var validationResult = new PolicyValidationResult(false, List.of("error1", "error2"));

        var result = transformer.transform(validationResult, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo(EDC_POLICY_VALIDATION_RESULT_TYPE);
        assertThat(result.getBoolean(EDC_POLICY_VALIDATION_RESULT_IS_VALID)).isFalse();
        assertThat(result.getJsonArray(EDC_POLICY_VALIDATION_RESULT_ERRORS)).hasSize(2)
                .contains(Json.createValue("error1"), Json.createValue("error2"));
    }
    
}
