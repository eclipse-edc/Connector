/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetSelectorExpressionTest {

    private AssetSelectorExpression expression;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization_emptyExpression() throws JsonProcessingException {
        expression = AssetSelectorExpression.Builder.newInstance().build();
        var json = objectMapper.writeValueAsString(expression);

        assertThat(json).isNotNull();
    }

    @Test
    void verify_selectAll() throws JsonProcessingException {
        expression = AssetSelectorExpression.SELECT_ALL;
        var json = objectMapper.writeValueAsString(expression);

        assertThat(json).contains("[]");
        var expression = objectMapper.readValue(json, AssetSelectorExpression.class);
        assertThat(expression.getCriteria()).isEmpty();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        expression = AssetSelectorExpression.Builder.newInstance()
                .constraint("name", "IN", "(bob, alice)")
                .build();
        var json = objectMapper.writeValueAsString(expression);
        assertThat(json).contains("name")
                .contains("IN")
                .contains("(bob, alice)");
    }

    @Test
    void verifyDeserialization() throws JsonProcessingException {
        expression = AssetSelectorExpression.Builder.newInstance()
                .constraint("name", "IN", "(bob, alice)")
                .build();
        var json = objectMapper.writeValueAsString(expression);

        var expr = objectMapper.readValue(json, AssetSelectorExpression.class);
        assertThat(expr.getCriteria()).hasSize(1)
                .allSatisfy(c -> {
                    assertThat(c.getOperandLeft()).isEqualTo("name");
                    assertThat(c.getOperator()).isEqualTo("IN");
                    assertThat(c.getOperandRight()).isEqualTo("(bob, alice)");
                });
    }
}