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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SqlQueryStatementTest {

    private static final String SELECT_STATEMENT = "SELECT * FROM test-table";
    private final CriterionToWhereClauseConverter criterionToWhereClauseConverter = mock();

    @Test
    void withoutQuerySpec_shouldSetOffsetAndLimit() {
        var statement = new SqlQueryStatement(SELECT_STATEMENT, 80, 20);

        assertThat(statement.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " LIMIT ? OFFSET ?;");
        assertThat(statement.getParameters()).containsOnly(80, 20);
        verifyNoInteractions(criterionToWhereClauseConverter);
    }

    @Test
    void withQuerySpec_shouldTranslateCriterionIntoWhereCondition() {
        var criterion = new Criterion("field1", "=", "testid1");
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new TestMapping(), criterionToWhereClauseConverter);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsOnly("testid1", 50, 0);
        verify(criterionToWhereClauseConverter).convert(criterion);
    }

    @Test
    void multipleExpressions() {
        var criterion1 = new Criterion("any", "=", "testid1");
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion1, criterion1), new TestMapping(), criterionToWhereClauseConverter);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? AND edc_field_1 = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactlyInAnyOrder("testid1", "testid1", 50, 0);
    }

    @Test
    void singleExpression_orderByDesc() {
        var criterion = new Criterion("field1", "=", "testid1");
        var builder = queryBuilder(criterion).sortField("description");
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, builder.sortOrder(SortOrder.DESC).build(), new TestMapping(), criterionToWhereClauseConverter);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? ORDER BY edc_description DESC LIMIT ? OFFSET ?;");
    }

    @Test
    void singleExpression_orderByAsc() {
        var criterion = new Criterion("field1", "=", "testid1");
        var builder = queryBuilder(criterion).sortField("description");
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, builder.sortOrder(SortOrder.ASC).build(), new TestMapping(), criterionToWhereClauseConverter);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? ORDER BY edc_description ASC LIMIT ? OFFSET ?;");
    }

    @Test
    void singleExpression_orderBy_WithNoCondition() {
        var builder = queryBuilder().sortField("description");
        var t = new SqlQueryStatement(SELECT_STATEMENT, builder.sortOrder(SortOrder.ASC).build(), new TestMapping(), criterionToWhereClauseConverter);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " ORDER BY edc_description ASC LIMIT ? OFFSET ?;");
    }

    @Test
    void singleExpression_orderBy_WithNonExistentProperty() {
        var builder = queryBuilder().sortField("notexist");

        assertThatThrownBy(() -> new SqlQueryStatement(SELECT_STATEMENT, builder.sortOrder(SortOrder.ASC).build(), new TestMapping(), criterionToWhereClauseConverter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot sort by");
    }

    @Test
    void addWhereClause() {
        var criterion = new Criterion("field1", "=", "testid1");
        var customParameter = 3;
        var customSql = "(another_field IS null OR (another_field IN (select * from another_table where that_field > ?))";
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new TestMapping(), criterionToWhereClauseConverter)
                .addWhereClause(customSql, customParameter);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? AND " + customSql + " LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactly("testid1", customParameter, 50, 0);
    }

    @Test
    void forUpdate() {
        var criterion = new Criterion("field1", "=", "testid1");
        var builder = queryBuilder(criterion).sortField("description");
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, builder.sortOrder(SortOrder.ASC).build(), new TestMapping(), criterionToWhereClauseConverter)
                .forUpdate();

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? ORDER BY edc_description ASC LIMIT ? OFFSET ? FOR UPDATE;");
    }

    @Test
    void forUpdate_skipLocked() {
        var criterion = new Criterion("field1", "=", "testid1");
        var builder = queryBuilder(criterion).sortField("description");
        when(criterionToWhereClauseConverter.convert(any())).thenReturn(new WhereClause("edc_field_1 = ?", "testid1"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, builder.sortOrder(SortOrder.ASC).build(), new TestMapping(), criterionToWhereClauseConverter)
                .forUpdate(true);

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? ORDER BY edc_description ASC LIMIT ? OFFSET ? FOR UPDATE SKIP LOCKED;");
    }

    private QuerySpec.Builder queryBuilder(Criterion... criterion) {
        return QuerySpec.Builder.newInstance().filter(List.of(criterion));
    }

    private QuerySpec query(Criterion... criterion) {
        return queryBuilder(criterion).build();
    }
}
