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

package org.eclipse.edc.connector.api.management.policy.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromPolicyDefinitionTransformerTest {

    private final JsonObjectFromPolicyDefinitionTransformer transformer = new JsonObjectFromPolicyDefinitionTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(PolicyDefinition.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var policy = Policy.Builder.newInstance().build();
        var input = PolicyDefinition.Builder.newInstance().id("definitionId").policy(policy).build();
        var policyJson = Json.createObjectBuilder().build();
        when(context.transform(any(), eq(JsonObject.class))).thenReturn(policyJson);

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("definitionId");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_POLICY_DEFINITION_TYPE);
        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_POLICY)).isSameAs(policyJson);
        verify(context).transform(policy, JsonObject.class);
    }

}
