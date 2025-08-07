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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transform.TestInput.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_REMEDY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToProhibitionTransformerTest {

    private final TransformerContext context = mock();
    private final Action action = Action.Builder.newInstance().type("type").build();
    private final Constraint constraint = AtomicConstraint.Builder.newInstance().build();

    private final JsonObjectToProhibitionTransformer transformer = new JsonObjectToProhibitionTransformer();

    @BeforeEach
    void setUp() {

        when(context.transform(isA(JsonObject.class), eq(Action.class))).thenReturn(action);
        when(context.transform(isA(JsonObject.class), eq(Constraint.class))).thenReturn(constraint);
    }

    @Test
    void transform_attributesAsObjects_returnPermission() {
        var remedy = Duty.Builder.newInstance().build();
        when(context.transform(isA(JsonObject.class), eq(Duty.class))).thenReturn(remedy);
        var prohibition = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, Json.createObjectBuilder().add(TYPE, "Action"))
                .add(ODRL_CONSTRAINT_ATTRIBUTE, Json.createObjectBuilder().add(TYPE, ODRL_CONSTRAINT_TYPE))
                .add(ODRL_REMEDY_ATTRIBUTE, Json.createObjectBuilder().add(ODRL_ACTION_ATTRIBUTE, "remedyAction"))
                .add(ODRL_TARGET_ATTRIBUTE, "target")
                .build();

        var result = transformer.transform(getExpanded(prohibition), context);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo(action);
        assertThat(result.getConstraints()).hasSize(1);
        assertThat(result.getConstraints().get(0)).isEqualTo(constraint);
        assertThat(result.getRemedies()).containsOnly(remedy);

        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Action.class));
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Constraint.class));
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Duty.class));
    }

}
