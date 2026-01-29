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

package org.eclipse.edc.policy.cel.store.sql;

import org.eclipse.edc.policy.cel.store.sql.postgres.CelExpressionMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatementsConfig implements CelExpressionStoreStatements {

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getLeftOperandColumn())
                .column(getExpressionColumn())
                .column(getDescriptionColumn())
                .jsonColumn(getScopesColumn())
                .column(getCreateTimestampColumn())
                .column(getLastModifiedTimestampColumn())
                .insertInto(getCelExpressionTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getLeftOperandColumn())
                .column(getExpressionColumn())
                .column(getDescriptionColumn())
                .jsonColumn(getScopesColumn())
                .column(getCreateTimestampColumn())
                .column(getLastModifiedTimestampColumn())
                .update(getCelExpressionTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getCelExpressionTable(), getIdColumn());

    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getCelExpressionTable(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new CelExpressionMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getCelExpressionTable());
    }
}
