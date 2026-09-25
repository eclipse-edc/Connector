/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.policy.cel.model;

import org.eclipse.edc.policy.model.Operator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CelExpressionTest {

    @Test
    void build_whenIdNotProvided_shouldGenerateUuid() {
        var expression = CelExpression.Builder.newInstance()
                .leftOperand("leftOperand")
                .expression("true")
                .description("description")
                .build();

        assertThat(expression.getId()).isNotNull();
        assertThatCode(() -> UUID.fromString(expression.getId())).doesNotThrowAnyException();
    }

    @Test
    void build_whenIdProvided_shouldKeepIt() {
        var expression = CelExpression.Builder.newInstance()
                .id("my-id")
                .leftOperand("leftOperand")
                .expression("true")
                .description("description")
                .build();

        assertThat(expression.getId()).isEqualTo("my-id");
    }

    @Test
    void build_whenBuiltTwice_shouldGenerateDifferentIds() {
        var first = CelExpression.Builder.newInstance()
                .leftOperand("leftOperand")
                .expression("true")
                .description("description")
                .build();

        var second = CelExpression.Builder.newInstance()
                .leftOperand("leftOperand")
                .expression("true")
                .description("description")
                .build();

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void supportsOperator_whenNoneConfigured_shouldSupportAll() {
        var expression = expression(Set.of());

        assertThat(Operator.values()).allMatch(expression::supportsOperator);
    }

    @Test
    void supportsOperator_whenConfigured_shouldSupportOnlyThose() {
        var expression = expression(Set.of(Operator.EQ, Operator.IS_PART_OF));

        assertThat(expression.supportsOperator(Operator.EQ)).isTrue();
        assertThat(expression.supportsOperator(Operator.IS_PART_OF)).isTrue();
        assertThat(expression.supportsOperator(Operator.NEQ)).isFalse();
    }

    @Test
    void supportsOperator_shouldTreatInAsIsPartOf() {
        assertThat(expression(Set.of(Operator.IS_PART_OF)).supportsOperator(Operator.IN)).isTrue();

        var legacy = expression(Set.of(Operator.IN));
        assertThat(legacy.getSupportedOperators()).containsExactly(Operator.IS_PART_OF);
        assertThat(legacy.supportsOperator(Operator.IS_PART_OF)).isTrue();
    }

    private CelExpression expression(Set<Operator> supportedOperators) {
        return CelExpression.Builder.newInstance()
                .leftOperand("leftOperand")
                .expression("true")
                .description("description")
                .supportedOperators(supportedOperators)
                .build();
    }
}
