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

package org.eclipse.edc.connector.controlplane.transform.edc.policy.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToPolicyDefinitionTransformerTest {

    private final JsonObjectToPolicyDefinitionTransformer transformer = new JsonObjectToPolicyDefinitionTransformer();
    private final TransformerContext context = mock(TransformerContext.class);
    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(PolicyDefinition.class);
    }

    @Test
    void transform() {
        var policyJson = createObjectBuilder().build();
        var policy = Policy.Builder.newInstance().build();
        when(context.transform(any(), eq(Policy.class))).thenReturn(policy);
        var json = createObjectBuilder()
                .add(ID, "definitionId")
                .add(TYPE, EDC_POLICY_DEFINITION_TYPE)
                .add(EDC_POLICY_DEFINITION_POLICY, policyJson)
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("definitionId");
        assertThat(result.getPolicy()).isSameAs(policy);
        verify(context).transform(policyJson, Policy.class);
    }

    @Test
    void transform_withPrivateProperties() {
        when(context.transform(any(), eq(Object.class))).thenReturn("test-val");
        var policyJson = createObjectBuilder().build();
        var policy = Policy.Builder.newInstance().build();
        when(context.transform(any(), eq(Policy.class))).thenReturn(policy);
        var json = createObjectBuilder()
                .add(CONTEXT, createContextBuilder().addNull(EDC_PREFIX).build())
                .add(ID, "definitionId")
                .add(TYPE, EDC_POLICY_DEFINITION_TYPE)
                .add(EDC_POLICY_DEFINITION_POLICY, policyJson)
                .add(PolicyDefinition.EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES,
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("test-prop", "test-val")
                                        .build())
                                .build())
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("definitionId");
        assertThat(result.getPolicy()).isSameAs(policy);
        verify(context).transform(policyJson, Policy.class);
        assertThat(result.getPrivateProperties())
                .hasSize(1)
                .containsEntry(EDC_NAMESPACE + "test-prop", "test-val");
    }

    @Test
    void shouldFailWhenPolicyCannotBeTransformed() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        var policyJson = createObjectBuilder().build();
        when(context.transform(any(), eq(Policy.class))).thenReturn(null);
        var json = createObjectBuilder()
                .add(ID, "definitionId")
                .add(TYPE, EDC_POLICY_DEFINITION_TYPE)
                .add(EDC_POLICY_DEFINITION_POLICY, policyJson)
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNull();
        verify(context).transform(policyJson, Policy.class);
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
