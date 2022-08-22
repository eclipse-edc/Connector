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

package org.eclipse.dataspaceconnector.sql.translation;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlQueryStatementTest {

    private static final String SELECT_STATEMENT = "SELECT * FROM test-table";

    @Test
    void singleExpression_equalsOperator() {
        var criterion = new Criterion("field1", "=", "testid1");
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new TestMapping());

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsOnly("testid1", 50, 0);
    }

    @Test
    void singleExpression_inOperator() {
        var criterion = new Criterion("field1", "in", List.of("id1", "id2", "id3"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new TestMapping());


        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 IN (?,?,?) LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactlyInAnyOrder("id1", "id2", "id3", 50, 0);
    }

    @Test
    void multipleExpressions() {
        var criterion1 = new Criterion("field1", "in", List.of("id1", "id2", "id3"));
        var criterion2 = new Criterion("description", "=", "something");
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion1, criterion2), new TestMapping());

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE edc_field_1 IN (?,?,?) AND edc_description = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactlyInAnyOrder("id1", "id2", "id3", "something", 50, 0);
    }

    private QuerySpec query(Criterion... criterion) {
        return QuerySpec.Builder.newInstance().filter(List.of(criterion)).build();
    }
}