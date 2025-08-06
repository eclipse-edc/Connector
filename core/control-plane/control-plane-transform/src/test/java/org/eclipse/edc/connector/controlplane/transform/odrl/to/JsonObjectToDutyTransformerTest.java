/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.odrl.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transform.TestInput;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSEQUENCE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToDutyTransformerTest {
    private static final String TARGET = "target";

    private final TransformerContext context = mock();

    private final Action action = Action.Builder.newInstance().type("type").build();
    private final Constraint constraint = AtomicConstraint.Builder.newInstance().build();
    private final Duty consequence = Duty.Builder.newInstance().build();

    private JsonObjectToDutyTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDutyTransformer();

        when(context.transform(isA(JsonObject.class), eq(Action.class))).thenReturn(action);
        when(context.transform(isA(JsonObject.class), eq(Constraint.class))).thenReturn(constraint);
        when(context.transform(isA(JsonObject.class), eq(Duty.class))).thenReturn(consequence);
    }

    @Test
    void transform_returnDuty() {
        var actionJson = createObjectBuilder().add(TYPE, "action");
        var constraintJson = createObjectBuilder().add(TYPE, "constraint");
        var consequencesJson = createArrayBuilder()
                .add(createObjectBuilder().add(TYPE, "consequence"))
                .add(createObjectBuilder().add(TYPE, "consequence"));
        var jsonDuty = createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionJson)
                .add(ODRL_CONSTRAINT_ATTRIBUTE, constraintJson)
                .add(ODRL_CONSEQUENCE_ATTRIBUTE, consequencesJson)
                .add(ODRL_TARGET_ATTRIBUTE, TARGET)
                .build();

        var result = transformer.transform(TestInput.getExpanded(jsonDuty), context);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo(action);
        assertThat(result.getConstraints()).hasSize(1);
        assertThat(result.getConstraints().get(0)).isEqualTo(constraint);
        assertThat(result.getConsequences()).hasSize(2).first().isEqualTo(consequence);

        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Action.class));
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Constraint.class));
        verify(context, times(2)).transform(isA(JsonObject.class), eq(Duty.class));
    }

}
