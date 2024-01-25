/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CriterionToWhereClauseConverterImplTest {

    private final SqlOperatorTranslator operatorTranslator = mock();
    private final CriterionToWhereClauseConverter converter = new CriterionToWhereClauseConverterImpl(new TestMapping(), operatorTranslator);

    @Test
    void shouldConvertCriterionToWhereClause() {
        when(operatorTranslator.translate("model-operator")).thenReturn(new SqlOperator("sql-operator", Object.class));
        var criterion = new Criterion("field1", "model-operator", "testid1");

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("edc_field_1 sql-operator ?");
        assertThat(condition.parameters()).containsExactly("testid1");
    }

    @Test
    void singleExpression_equalsOperator() {
        when(operatorTranslator.translate("=")).thenReturn(new SqlOperator("=", Object.class));
        var criterion = new Criterion("field1", "=", "testid1");

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("edc_field_1 = ?");
        assertThat(condition.parameters()).containsExactly("testid1");
    }

    @Test
    void singleExpression_equalsOperator_integer() {
        when(operatorTranslator.translate("=")).thenReturn(new SqlOperator("=", Object.class));
        var criterion = new Criterion("field1", "=", 3);

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("edc_field_1 = ?");
        assertThat(condition.parameters()).containsExactly(3);
    }

    @Test
    void singleExpression_notExistentColumn() {
        when(operatorTranslator.translate("=")).thenReturn(new SqlOperator("=", Object.class));
        var criterion = new Criterion("not-existent", "=", "testid1");

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("0 = ?");
        assertThat(condition.parameters()).containsExactly(1);
    }

    @Test
    void singleExpression_inOperator() {
        when(operatorTranslator.translate("in")).thenReturn(new SqlOperator("IN", Collection.class));
        var criterion = new Criterion("field1", "in", List.of("id1", "id2", "id3"));

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("edc_field_1 IN (?,?,?)");
        assertThat(condition.parameters()).containsExactly("id1", "id2", "id3");
    }

    @Test
    void shouldReturnAlwaysFalseClause_whenMappingNotFound() {
        when(operatorTranslator.translate("=")).thenReturn(new SqlOperator("=", Object.class));
        var criterion = new Criterion("not-existing-field", "=", "testid1");

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("0 = ?");
        assertThat(condition.parameters()).containsExactly(1);
    }

    @Test
    void shouldLowerCaseInputOperator() {
        when(operatorTranslator.translate(any())).thenReturn(new SqlOperator("in", Collection.class));
        var criterion = new Criterion("field1", "IN", List.of("id1", "id2", "id3"));

        converter.convert(criterion);

        verify(operatorTranslator).translate("in");
    }

    @Test
    void shouldThrowException_whenOperatorIsNotKnown() {
        when(operatorTranslator.translate(any())).thenReturn(null);
        var criterion = Criterion.criterion("description", "unknown", "something");

        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowException_whenRightOperandTypeNotSupportedByOperator() {
        when(operatorTranslator.translate("like")).thenReturn(new SqlOperator("like", String.class));
        var criterion = Criterion.criterion("description", "like", 3);

        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class);
    }
}
