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

package org.eclipse.edc.connector.controlplane.transform.edc.cel.from;

import jakarta.json.Json;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_DESCRIPTION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_EXPRESSION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_LEFT_OPERAND_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_SCOPES_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_TYPE_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.MATCH_ALL_SCOPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromCelExpressionTransformerTest {

    private JsonObjectFromCelExpressionTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCelExpressionTransformer(Json.createBuilderFactory(null));
        context = mock(TransformerContext.class);
    }

    @Test
    void transform_shouldConvertCelExpressionToJsonObject() {
        var celExpression = CelExpression.Builder.newInstance()
                .id("test-id")
                .leftOperand("user.role")
                .expression("== 'admin'")
                .description("Check if user is admin")
                .scopes(Set.of("read", "write"))
                .build();

        var result = transformer.transform(celExpression, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("test-id");
        assertThat(result.getString(TYPE)).isEqualTo(CEL_EXPRESSION_TYPE_IRI);
        assertThat(result.getString(CEL_EXPRESSION_LEFT_OPERAND_IRI)).isEqualTo("user.role");
        assertThat(result.getString(CEL_EXPRESSION_EXPRESSION_IRI)).isEqualTo("== 'admin'");
        assertThat(result.getString(CEL_EXPRESSION_DESCRIPTION_IRI)).isEqualTo("Check if user is admin");
        assertThat(result.getJsonArray(CEL_EXPRESSION_SCOPES_IRI))
                .hasSize(2)
                .containsExactlyInAnyOrder(Json.createValue("read"), Json.createValue("write"));
    }

    @Test
    void transform_shouldHandleEmptyScopes() {
        var celExpression = CelExpression.Builder.newInstance()
                .id("test-id")
                .leftOperand("user.role")
                .expression("== 'admin'")
                .description("Test")
                .build();

        var result = transformer.transform(celExpression, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(CEL_EXPRESSION_SCOPES_IRI))
                .contains(Json.createValue(MATCH_ALL_SCOPE));
    }
}
