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

package org.eclipse.edc.protocol.dsp.transform.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_POLICY_TYPE_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToPolicyTransformerTest {
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectToPolicyTransformer transformer;
    
    private JsonObject permissionJson;
    private JsonObject prohibitionJson;
    private JsonObject dutyJson;
    
    private Permission permission;
    private Prohibition prohibition;
    private Duty duty;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToPolicyTransformer();
    
        permissionJson = getJsonObject("permission");
        prohibitionJson = getJsonObject("prohibition");
        dutyJson = getJsonObject("duty");
        
        permission = Permission.Builder.newInstance().build();
        prohibition = Prohibition.Builder.newInstance().build();
        duty = Duty.Builder.newInstance().build();
        
        when(context.transform(permissionJson, Permission.class)).thenReturn(permission);
        when(context.transform(prohibitionJson, Prohibition.class)).thenReturn(prohibition);
        when(context.transform(dutyJson, Duty.class)).thenReturn(duty);
    }
    
    @Test
    void transform_withAllRuleTypesAsObjects_returnPolicy() {
        var policy = jsonFactory.createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add(ODRL_PERMISSION_ATTRIBUTE, permissionJson)
                .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionJson)
                .add(ODRL_OBLIGATION_ATTRIBUTE, dutyJson)
                .build();
    
        var result = transformer.transform(policy, context);
    
        assertResult(result);
    }
    
    @Test
    void transform_withAllRuleTypesAsArrays_returnPolicy() {
        var policy = jsonFactory.createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add(ODRL_PERMISSION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(permissionJson))
                .add(ODRL_PROHIBITION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(prohibitionJson))
                .add(ODRL_OBLIGATION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(dutyJson))
                .build();
        
        var result = transformer.transform(policy, context);
        
        assertResult(result);
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
        
        var result = transformer.transform(policy, context);
        
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
                .add(TYPE, type)
                .build();
        
        var result = transformer.transform(policy, context);
        
        assertThat(result).isNotNull();
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_invalidType_reportProblem() {
        var policy = jsonFactory.createObjectBuilder()
                .add(TYPE, "not-a-policy")
                .build();
    
        transformer.transform(policy, context);
    
        verify(context, times(1)).reportProblem(anyString());
    }
    
    private JsonObject getJsonObject(String type) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
    
    private void assertResult(Policy result) {
        assertThat(result).isNotNull();
        assertThat(result.getPermissions()).hasSize(1);
        assertThat(result.getPermissions().get(0)).isEqualTo(permission);
        assertThat(result.getProhibitions()).hasSize(1);
        assertThat(result.getProhibitions().get(0)).isEqualTo(prohibition);
        assertThat(result.getObligations()).hasSize(1);
        assertThat(result.getObligations().get(0)).isEqualTo(duty);
        
        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(permissionJson, Permission.class);
        verify(context, times(1)).transform(prohibitionJson, Prohibition.class);
        verify(context, times(1)).transform(dutyJson, Duty.class);
    }
}
