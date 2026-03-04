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

package org.eclipse.edc.catalog.cache.sql;

import org.eclipse.edc.catalog.cache.sql.schema.postgres.FederatedCatalogMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public abstract class BaseSqlDialectStatements implements FederatedCatalogCacheStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    protected BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getFederatedCatalogTable(), getIdColumn());
    }

    @Override
    public String getUpdateAsMarkedTemplate() {
        return format("UPDATE %s SET %s = ?", getFederatedCatalogTable(), getMarkedColumn());
    }

    @Override
    public String getDeleteByMarkedTemplate() {
        return executeStatement()
                .delete(getFederatedCatalogTable(), getMarkedColumn());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .jsonColumn(getCatalogColumn())
                .column(getMarkedColumn())
                .insertInto(getFederatedCatalogTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .jsonColumn(getCatalogColumn())
                .column(getMarkedColumn())
                .update(getFederatedCatalogTable(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new FederatedCatalogMapping(this), operatorTranslator);
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getFederatedCatalogTable());
    }
}
