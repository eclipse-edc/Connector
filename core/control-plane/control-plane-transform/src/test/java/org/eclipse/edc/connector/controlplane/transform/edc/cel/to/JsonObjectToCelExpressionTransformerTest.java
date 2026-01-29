/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.cel.to;

import jakarta.json.Json;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_DESCRIPTION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_EXPRESSION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_LEFT_OPERAND_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_SCOPES_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.MATCH_ALL_SCOPE;
import static org.mockito.Mockito.mock;

class JsonObjectToCelExpressionTransformerTest {

    private JsonObjectToCelExpressionTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToCelExpressionTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void transform_shouldConvertJsonObjectToCelExpression() {
        var json = Json.createObjectBuilder()
                .add(ID, "test-id")
                .add(CEL_EXPRESSION_LEFT_OPERAND_IRI, "user.role")
                .add(CEL_EXPRESSION_EXPRESSION_IRI, "== 'admin'")
                .add(CEL_EXPRESSION_DESCRIPTION_IRI, "Check if user is admin")
                .add(CEL_EXPRESSION_SCOPES_IRI, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add(VALUE, "read"))
                        .add(Json.createObjectBuilder().add(VALUE, "write")))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-id");
        assertThat(result.getLeftOperand()).isEqualTo("user.role");
        assertThat(result.getExpression()).isEqualTo("== 'admin'");
        assertThat(result.getDescription()).isEqualTo("Check if user is admin");
        assertThat(result.getScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void transform_shouldGenerateIdWhenMissing() {
        var json = Json.createObjectBuilder()
                .add(CEL_EXPRESSION_LEFT_OPERAND_IRI, "user.role")
                .add(CEL_EXPRESSION_EXPRESSION_IRI, "== 'admin'")
                .add(CEL_EXPRESSION_DESCRIPTION_IRI, "description")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull().isNotEmpty();
    }

    @Test
    void transform_shouldHandleEmptyScopes() {
        var json = Json.createObjectBuilder()
                .add(ID, "test-id")
                .add(CEL_EXPRESSION_LEFT_OPERAND_IRI, "user.role")
                .add(CEL_EXPRESSION_EXPRESSION_IRI, "== 'admin'")
                .add(CEL_EXPRESSION_DESCRIPTION_IRI, "description")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getScopes()).containsExactly(MATCH_ALL_SCOPE);
    }

}
