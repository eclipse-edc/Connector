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
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.VALUE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToConstraintTransformerTest {
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectToConstraintTransformer transformer;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToConstraintTransformer();
    }
    
    @Test
    void transform_attributesAsObjects_returnConstraint() {
        var left = "left";
        var operator = Operator.EQ;
        var right = "right";
        
        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, left))
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, operator.name()))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, right))
                .build();
        
        var result = (AtomicConstraint) transformer.transform(constraint, context);
        
        assertThat(result).isNotNull();
        assertThat(((LiteralExpression) result.getLeftExpression()).getValue()).isEqualTo(left);
        assertThat(result.getOperator()).isEqualTo(operator);
        assertThat(((LiteralExpression) result.getRightExpression()).getValue()).isEqualTo(right);
        
        verifyNoInteractions(context);
    }
    
    @Test
    void transform_attributesAsArrays_returnConstraint() {
        var left = "left";
        var operator = Operator.EQ;
        var right = "right";
        
        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createArrayBuilder()
                        .add(jsonFactory.createObjectBuilder()
                                .add(VALUE, left)))
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonFactory.createArrayBuilder()
                        .add(jsonFactory.createObjectBuilder()
                                .add(VALUE, operator.name())))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createArrayBuilder()
                        .add(jsonFactory.createObjectBuilder()
                                .add(VALUE, right)))
                .build();
        
        var result = (AtomicConstraint) transformer.transform(constraint, context);
        
        assertThat(result).isNotNull();
        assertThat(((LiteralExpression) result.getLeftExpression()).getValue()).isEqualTo(left);
        assertThat(result.getOperator()).isEqualTo(operator);
        assertThat(((LiteralExpression) result.getRightExpression()).getValue()).isEqualTo(right);
        
        verifyNoInteractions(context);
    }
    
    @Test
    void transform_operatorAsString_returnConstraint() {
        var left = "left";
        var operator = Operator.EQ;
        var right = "right";
        
        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, left))
                .add(ODRL_OPERATOR_ATTRIBUTE, operator.name())
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, right))
                .build();
        
        var result = (AtomicConstraint) transformer.transform(constraint, context);
        
        assertThat(result).isNotNull();
        assertThat(result.getOperator()).isEqualTo(operator);
        
        verifyNoInteractions(context);
    }
    
    @Test
    void transform_invalidAttributes_reportProblems() {
        var jsonNumber = Json.createValue(1);
        
        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonNumber)
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonNumber)
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonNumber)
                .build();
    
        transformer.transform(constraint, context);
        
        verify(context, times(3)).reportProblem(anyString());
    }
}
