/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import org.eclipse.dataspaceconnector.cosmos.azure.TestCosmosDocument;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlStatementTest {

    @Test
    void getQuerySpec() {
        var stmt = new SqlStatement<>(TestCosmosDocument.class);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument");
    }

    @Test
    void getQuerySpec_withWhere() {
        var stmt = new SqlStatement<>(TestCosmosDocument.class);
        stmt.where(List.of(new Criterion("id", "=", "1")));
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id");
        assertThat(stmt.getParameters()).hasSize(1).allSatisfy(p -> {
            assertThat(p.getName()).isEqualTo("@id");
            assertThat(p.getValue(String.class)).isEqualTo("1");
        });
    }

    @Test
    void getQuerySpec_withWhereOrderBy() {
        var stmt = new SqlStatement<>(TestCosmosDocument.class)
                .where(List.of(new Criterion("id", "=", "1")))
                .orderBy("priority");
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id ORDER BY TestCosmosDocument.wrappedInstance.priority ASC");

        stmt.orderBy("priority", false);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id ORDER BY TestCosmosDocument.wrappedInstance.priority DESC");
    }

    @Test
    void getQuerySpec_withWhereOrderByLimit() {
        var stmt = new SqlStatement<>(TestCosmosDocument.class)
                .where(List.of(new Criterion("id", "=", "1")))
                .orderBy("priority")
                .limit(15);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id ORDER BY TestCosmosDocument.wrappedInstance.priority ASC LIMIT 15");

        stmt.orderBy("priority", false);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id ORDER BY TestCosmosDocument.wrappedInstance.priority DESC LIMIT 15");
    }

    @Test
    void getQuerySpec_withWhereOrderByLimitOffset() {
        var stmt = new SqlStatement<>(TestCosmosDocument.class)
                .where(List.of(new Criterion("id", "=", "1")))
                .orderBy("priority")
                .offset(10)
                .limit(15);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id ORDER BY TestCosmosDocument.wrappedInstance.priority ASC OFFSET 10 LIMIT 15");

        stmt.orderBy("priority", false);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument WHERE TestCosmosDocument.wrappedInstance.id = @id ORDER BY TestCosmosDocument.wrappedInstance.priority DESC OFFSET 10 LIMIT 15");
    }

    @Test
    void getQuerySpec_withLimitOffset() {
        var stmt = new SqlStatement<>(TestCosmosDocument.class)
                .orderBy("priority")
                .offset(10)
                .limit(15);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument ORDER BY TestCosmosDocument.wrappedInstance.priority ASC OFFSET 10 LIMIT 15");

        stmt.orderBy("priority", false);
        assertThat(stmt.getQueryAsString()).isEqualTo("SELECT * FROM TestCosmosDocument ORDER BY TestCosmosDocument.wrappedInstance.priority DESC OFFSET 10 LIMIT 15");
    }
}