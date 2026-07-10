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

import org.junit.jupiter.api.Test;

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
}
