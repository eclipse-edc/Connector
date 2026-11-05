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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;

public abstract class BaseSqlDialectStatements implements FederatedCatalogCacheStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    protected BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ? AND %s = ?", getFederatedCatalogTable(), getParticipantContextIdColumn(), getIdColumn());
    }

    @Override
    public String getUpdateAsMarkedTemplate() {
        return format("UPDATE %s SET %s = ? WHERE %s = ?", getFederatedCatalogTable(), getMarkedColumn(), getParticipantContextIdColumn());
    }

    @Override
    public String getDeleteByMarkedTemplate() {
        return executeStatement()
                .delete(getFederatedCatalogTable(), equalTo(getParticipantContextIdColumn()), equalTo(getMarkedColumn()));
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getParticipantContextIdColumn())
                .jsonColumn(getCatalogColumn())
                .column(getMarkedColumn())
                .insertInto(getFederatedCatalogTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .jsonColumn(getCatalogColumn())
                .column(getMarkedColumn())
                .update(getFederatedCatalogTable(), equalTo(getParticipantContextIdColumn()), equalTo(getIdColumn()));
    }

    @Override
    public SqlQueryStatement createQuery(String participantContextId, QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, scopedQuery(participantContextId, querySpec), new FederatedCatalogMapping(this), operatorTranslator);
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getFederatedCatalogTable());
    }

    /**
     * Adds a filter on the {@code participantContextId} column so that only catalogs owned by the given participant
     * context are returned.
     */
    protected QuerySpec scopedQuery(String participantContextId, QuerySpec querySpec) {
        return querySpec.toBuilder()
                .filter(new Criterion("participantContextId", "=", participantContextId))
                .build();
    }
}
