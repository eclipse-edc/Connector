/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory.sql;

import org.eclipse.edc.catalog.directory.sql.schema.postgres.TargetNodeMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public abstract class BaseSqlDialectStatements implements TargetNodeStatements {

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getTargetNodeDirectoryTable(), getIdColumn());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getNameColumn())
                .column(getTargetUrlColumn())
                .jsonColumn(getSupportedProtocolsColumn())
                .update(getTargetNodeDirectoryTable(), getIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getNameColumn())
                .column(getTargetUrlColumn())
                .jsonColumn(getSupportedProtocolsColumn())
                .insertInto(getTargetNodeDirectoryTable());
    }

    @Override
    public String getDeleteTemplate() {
        return executeStatement()
                .delete(getTargetNodeDirectoryTable(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new TargetNodeMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getTargetNodeDirectoryTable());
    }
}
