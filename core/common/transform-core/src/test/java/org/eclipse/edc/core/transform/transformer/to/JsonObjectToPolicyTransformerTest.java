/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.core.transform.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.core.transform.transformer.TestInput.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_AGREEMENT;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToPolicyTransformerTest {
    private static final String TARGET = "target";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);


    private final Permission permission = Permission.Builder.newInstance().build();
    private final Prohibition prohibition = Prohibition.Builder.newInstance().build();
    private final Duty duty = Duty.Builder.newInstance().build();

    private JsonObjectToPolicyTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToPolicyTransformer();

        when(context.transform(isA(JsonObject.class), eq(Permission.class))).thenReturn(permission);
        when(context.transform(isA(JsonObject.class), eq(Prohibition.class))).thenReturn(prohibition);
        when(context.transform(isA(JsonObject.class), eq(Duty.class))).thenReturn(duty);
    }


    @ParameterizedTest
    @MethodSource("jsonSource")
    void transform_withAllRuleTypesAsObjects_returnPolicy(JsonObject policy) {

        var result = transformer.transform(getExpanded(policy), context);

        assertThat(result).isNotNull();
        assertThat(result.getTarget()).isEqualTo(TARGET);
        assertThat(result.getPermissions()).hasSize(1);
        assertThat(result.getPermissions().get(0)).isEqualTo(permission);
        assertThat(result.getProhibitions()).hasSize(1);
        assertThat(result.getProhibitions().get(0)).isEqualTo(prohibition);
        assertThat(result.getObligations()).hasSize(1);
        assertThat(result.getObligations().get(0)).isEqualTo(duty);

        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Permission.class));
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Prohibition.class));
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Duty.class));

    }

    @Test
    void transform_policyWithAdditionalProperty_returnPolicy() {
        var propertyKey = "policy:prop:key";
        var propertyValue = "value";

        when(context.transform(any(JsonValue.class), eq(Object.class))).thenReturn(propertyValue);

        var policy = jsonFactory.createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add(propertyKey, propertyValue)
                .build();

        var result = transformer.transform(getExpanded(policy), context);

        assertThat(result).isNotNull();
        assertThat(result.getExtensibleProperties()).hasSize(1);
        assertThat(result.getExtensibleProperties()).containsEntry(propertyKey, propertyValue);

        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(any(JsonValue.class), eq(Object.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ODRL_POLICY_TYPE_SET,
            ODRL_POLICY_TYPE_OFFER,
            ODRL_POLICY_TYPE_AGREEMENT
    })
    void transform_differentPolicyTypes_returnPolicy(String type) {
        var policy = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, JsonObject.EMPTY_JSON_OBJECT)
                .add(TYPE, type)
                .build();

        var result = transformer.transform(getExpanded(policy), context);

        assertThat(result).isNotNull();
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_invalidType_reportProblem() {
        var policy = jsonFactory.createObjectBuilder()
                .add(TYPE, "not-a-policy")
                .build();

        transformer.transform(getExpanded(policy), context);

        verify(context, never()).reportProblem(anyString());
    }


    static Stream<JsonObject> jsonSource() {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        var permissionJson = jsonFactory.createObjectBuilder().add(TYPE, "permission").build();
        var prohibitionJson = jsonFactory.createObjectBuilder().add(TYPE, "prohibition").build();
        var dutyJson = jsonFactory.createObjectBuilder().add(TYPE, "duty").build();

        return Stream.of(
                jsonFactory.createObjectBuilder()
                        .add(TYPE, ODRL_POLICY_TYPE_SET)
                        .add(ODRL_TARGET_ATTRIBUTE, TARGET)
                        .add(ODRL_PERMISSION_ATTRIBUTE, permissionJson)
                        .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionJson)
                        .add(ODRL_OBLIGATION_ATTRIBUTE, dutyJson)
                        .build(),
                jsonFactory.createObjectBuilder()
                        .add(TYPE, ODRL_POLICY_TYPE_SET)
                        .add(ODRL_TARGET_ATTRIBUTE, jsonFactory.createArrayBuilder().add(TARGET))
                        .add(ODRL_PERMISSION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(permissionJson))
                        .add(ODRL_PROHIBITION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(prohibitionJson))
                        .add(ODRL_OBLIGATION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(dutyJson))
                        .build()

        );
    }

}
