/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.util.reflection.PathItem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class PlainColumnFieldTranslatorTest {

    private final PlainColumnFieldTranslator translator = new PlainColumnFieldTranslator("column_name");

    @Test
    void shouldReturnWhereClause_whenOperatorSpecified() {
        var path = PathItem.parse("any");

        var result = translator.toWhereClause(path, criterion("field", "=", "value"), createOperator());

        assertThat(result.sql()).isEqualTo("column_name any ?");
        assertThat(result.parameters()).containsExactly("value");
    }

    @Test
    void shouldMapMultipleParameters_whenRightOperandIsCollection() {
        var path = PathItem.parse("any");

        var result = translator.toWhereClause(path, criterion("field", "in", List.of("value", "another")), createOperator());

        assertThat(result.sql()).isEqualTo("column_name any (?,?)");
        assertThat(result.parameters()).containsExactly("value", "another");
    }

    @NotNull
    private static SqlOperator createOperator() {
        return new SqlOperator("any", Object.class);
    }
}
