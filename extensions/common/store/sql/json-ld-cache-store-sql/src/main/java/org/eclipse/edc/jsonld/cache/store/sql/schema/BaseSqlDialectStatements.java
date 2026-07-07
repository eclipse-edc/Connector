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

package org.eclipse.edc.jsonld.cache.store.sql.schema;

import org.eclipse.edc.jsonld.cache.store.sql.schema.postgres.CachedJsonLdContextMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public class BaseSqlDialectStatements implements CachedJsonLdContextStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getTable());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getUrlColumn())
                .jsonColumn(getContentColumn())
                .column(getPullStrategyColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .insertInto(getTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getUrlColumn())
                .jsonColumn(getContentColumn())
                .column(getPullStrategyColumn())
                .column(getUpdatedAtColumn())
                .update(getTable(), getIdColumn());
    }

    @Override
    public String getDeleteTemplate() {
        return executeStatement().delete(getTable(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new CachedJsonLdContextMapping(this), operatorTranslator);
    }
}
