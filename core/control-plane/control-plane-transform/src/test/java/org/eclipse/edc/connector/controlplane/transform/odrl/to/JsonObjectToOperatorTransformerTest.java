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
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToOperatorTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToOperatorTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToOperatorTransformer();
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void shouldTransformOperator_whenValidOperator() {
        var jsonOperator = jsonFactory.createObjectBuilder().add(ID, ODRL_SCHEMA + "eq").build();

        var operator = transformer.transform(jsonOperator, context);

        assertThat(operator).isEqualTo(Operator.EQ);
    }

    @Test
    void shouldReportError_whenValueIsMissing() {
        var jsonOperator = jsonFactory.createObjectBuilder().build();

        var operator = transformer.transform(jsonOperator, context);

        assertThat(operator).isNull();
        verify(context).reportProblem(any());
    }

    @Test
    void shouldReportError_whenOperatorIsNotRecognized() {
        var jsonOperator = jsonFactory.createObjectBuilder().add(ID, "not-existent").build();

        var operator = transformer.transform(jsonOperator, context);

        assertThat(operator).isNull();
        verify(context).reportProblem(any());
    }
}
