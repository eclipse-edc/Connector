/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.entity.StateResolver;
import org.eclipse.edc.util.reflection.PathItem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityStateFieldTranslatorTest {

    private final StateResolver stateResolver = mock();
    private final EntityStateFieldTranslator translator = new EntityStateFieldTranslator("column_name", stateResolver);

    @Test
    void shouldReturnWhereClause_whenOperatorSpecified() {
        var path = PathItem.parse("any");

        var result = translator.toWhereClause(path, criterion("field", "=", 100), createOperator());

        assertThat(result.sql()).isEqualTo("column_name any ?");
        assertThat(result.parameters()).containsExactly(100);
    }

    @Test
    void shouldMapMultipleParameters_whenRightOperandIsCollection() {
        var path = PathItem.parse("any");

        var result = translator.toWhereClause(path, criterion("field", "in", List.of(100, 200)), createOperator());

        assertThat(result.sql()).isEqualTo("column_name any (?,?)");
        assertThat(result.parameters()).containsExactly(100, 200);
    }

    @Test
    void shouldTranslateStateToCode_whenInputTypeIsString() {
        when(stateResolver.resolve(any())).thenReturn(100);
        var path = PathItem.parse("any");

        var result = translator.toWhereClause(path, criterion("field", "=", "STATE"), createOperator());

        assertThat(result.sql()).isEqualTo("column_name any ?");
        assertThat(result.parameters()).containsExactly(100);
        verify(stateResolver).resolve("STATE");
    }

    @Test
    void shouldMapMultipleParametersToCode_whenRightOperandIsStringCollection() {
        when(stateResolver.resolve(any())).thenReturn(100).thenReturn(200);
        var path = PathItem.parse("any");

        var result = translator.toWhereClause(path, criterion("field", "in", List.of("STATE", "ANOTHER_STATE")), createOperator());

        assertThat(result.sql()).isEqualTo("column_name any (?,?)");
        assertThat(result.parameters()).containsExactly(100, 200);
        verify(stateResolver).resolve("STATE");
        verify(stateResolver).resolve("ANOTHER_STATE");
    }

    @NotNull
    private static SqlOperator createOperator() {
        return new SqlOperator("any", Object.class);
    }
}
