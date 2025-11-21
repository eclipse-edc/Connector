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

package org.eclipse.edc.connector.controlplane.transform.edc.policy.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromPolicyDefinitionTransformerTest {

    private final TypeManager typeManager = mock();
    private final JsonObjectFromPolicyDefinitionTransformer transformer = new JsonObjectFromPolicyDefinitionTransformer(Json.createBuilderFactory(emptyMap()), typeManager, "test");
    private final TransformerContext context = mock(TransformerContext.class);

    @BeforeEach
    void setup() {
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

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

    @Test
    void transform_withPrivateProperties_simpleTypes() {
        var policy = Policy.Builder.newInstance().build();
        var input = PolicyDefinition.Builder.newInstance().id("definitionId").policy(policy).privateProperty("some-key", "some-value").build();
        var policyJson = Json.createObjectBuilder().build();
        when(context.transform(any(), eq(JsonObject.class))).thenReturn(policyJson);

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("definitionId");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_POLICY_DEFINITION_TYPE);
        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_POLICY)).isSameAs(policyJson);

        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES).getJsonString("some-key").getString()).isEqualTo("some-value");
        verify(context).transform(policy, JsonObject.class);
    }


    @Test
    void transform_withPrivateProperties_complexTypes() {
        var policy = Policy.Builder.newInstance().build();
        var input = PolicyDefinition
                .Builder.newInstance()
                .id("definitionId")
                .policy(policy)
                .privateProperty("root", Map.of("key1", "value1", "nested1", Map.of("key2", "value2", "key3", Map.of("theKey", "theValue, this is what we're looking for"))))
                .build();
        var policyJson = Json.createObjectBuilder().build();
        when(context.transform(any(), eq(JsonObject.class))).thenReturn(policyJson);

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("definitionId");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_POLICY_DEFINITION_TYPE);
        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_POLICY)).isSameAs(policyJson);

        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES)
                .getJsonObject("root")
                .getJsonString("key1")
                .getString())
                .isEqualTo("value1");
        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES)
                .getJsonObject("root")
                .getJsonObject("nested1")
                .getJsonString("key2")
                .getString())
                .isEqualTo("value2");
        assertThat(result.getJsonObject(EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES)
                .getJsonObject("root")
                .getJsonObject("nested1")
                .getJsonObject("key3")
                .getJsonString("theKey")
                .getString())
                .isEqualTo("theValue, this is what we're looking for");

        verify(context).transform(policy, JsonObject.class);
    }
}
