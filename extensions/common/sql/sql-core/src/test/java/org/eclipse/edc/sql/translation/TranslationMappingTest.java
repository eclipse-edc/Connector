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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TranslationMappingTest {

    private final FieldTranslator fieldTranslator = mock();
    private final FieldTranslator nestedFieldTranslator = mock();
    private final TranslationMapping mapping = new TestMapping(fieldTranslator, nestedFieldTranslator);

    @Test
    void shouldInvokeTranslatorAndReturnWhereClause() {
        var expected = new WhereClause("column = ?", "value");
        when(fieldTranslator.toWhereClause(any(), any(), any())).thenReturn(expected);
        var criterion = criterion("field", "=", "value");

        var whereClause = mapping.getWhereClause(criterion, dummyOperator());

        assertThat(whereClause).isSameAs(expected);
        verify(fieldTranslator).toWhereClause(argThat(List::isEmpty), same(criterion), any());
    }

    @Test
    void shouldReturnNull_whenFieldDoesNotExist() {
        var whereClause = mapping.getWhereClause(criterion("notExistentField", "=", "value"), dummyOperator());

        assertThat(whereClause).isNull();
        verifyNoInteractions(fieldTranslator);
    }

    @Test
    void shouldInvokeNestedTranslatorAndReturnWhereClause() {
        var expected = new WhereClause("nested -> column = ?", "value");
        when(nestedFieldTranslator.toWhereClause(any(), any(), any())).thenReturn(expected);
        var criterion = criterion("nested.field", "=", "value");

        var whereClause = mapping.getWhereClause(criterion, dummyOperator());

        assertThat(whereClause).isSameAs(expected);
        verify(nestedFieldTranslator).toWhereClause(argThat(List::isEmpty), same(criterion), any());
    }

    @NotNull
    private SqlOperator dummyOperator() {
        return new SqlOperator("=", Object.class);
    }

    private static class TestMapping extends TranslationMapping {

        TestMapping(FieldTranslator fieldTranslator, FieldTranslator nestedFieldTranslator) {
            add("field", fieldTranslator);
            if (nestedFieldTranslator != null) {
                add("nested", new TestMapping(nestedFieldTranslator, null));
            }
        }
    }
}
