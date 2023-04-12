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
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_CONSEQUENCE_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToDutyTransformerTest {
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectToDutyTransformer transformer;
    
    private JsonObject actionJson;
    private JsonObject constraintJson;
    private JsonObject consequenceJson;
    
    private Action action;
    private Constraint constraint;
    private Duty consequence;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDutyTransformer();
    
        actionJson = getJsonObject("action");
        constraintJson = getJsonObject("constraint");
        consequenceJson = getJsonObject("consequence");
        
        action = Action.Builder.newInstance().type("type").build();
        constraint = AtomicConstraint.Builder.newInstance().build();
        consequence = Duty.Builder.newInstance().build();
    
        when(context.transform(actionJson, Action.class)).thenReturn(action);
        when(context.transform(constraintJson, Constraint.class)).thenReturn(constraint);
        when(context.transform(consequenceJson, Duty.class)).thenReturn(consequence);
    }
    
    @Test
    void transform_attributesAsObjects_returnDuty() {
        var duty = jsonFactory.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionJson)
                .add(ODRL_CONSTRAINT_ATTRIBUTE, constraintJson)
                .add(ODRL_CONSEQUENCE_ATTRIBUTE, consequenceJson)
                .build();
    
        var result = transformer.transform(duty, context);
    
        assertResult(result);
    }
    
    @Test
    void transform_attributesAsArrays_returnDuty() {
        var duty = jsonFactory.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, jsonFactory.createArrayBuilder().add(actionJson))
                .add(ODRL_CONSTRAINT_ATTRIBUTE, jsonFactory.createArrayBuilder().add(constraintJson))
                .add(ODRL_CONSEQUENCE_ATTRIBUTE, jsonFactory.createArrayBuilder().add(consequenceJson))
                .build();
        
        var result = transformer.transform(duty, context);
        
        assertResult(result);
    }
    
    private void assertResult(Duty result) {
        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo(action);
        assertThat(result.getConstraints()).hasSize(1);
        assertThat(result.getConstraints().get(0)).isEqualTo(constraint);
        assertThat(result.getConsequence()).isEqualTo(consequence);
    
        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(actionJson, Action.class);
        verify(context, times(1)).transform(constraintJson, Constraint.class);
        verify(context, times(1)).transform(consequenceJson, Duty.class);
    }
    
    private JsonObject getJsonObject(String type) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
    
}
