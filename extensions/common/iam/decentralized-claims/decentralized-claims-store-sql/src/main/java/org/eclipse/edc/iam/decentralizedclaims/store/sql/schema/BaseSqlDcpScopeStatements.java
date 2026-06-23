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

package org.eclipse.edc.iam.decentralizedclaims.store.sql.schema;

import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.postgres.DcpScopeMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public class BaseSqlDcpScopeStatements implements DcpScopeStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDcpScopeStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getTypeColumn())
                .column(getValueColumn())
                .column(getProfileColumn())
                .column(getPrefixMappingColumn())
                .insertInto(getTableName());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getTypeColumn())
                .column(getValueColumn())
                .column(getProfileColumn())
                .column(getPrefixMappingColumn())
                .update(getTableName(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getTableName());
    }

    @Override
    public String getDeleteTemplate() {
        return executeStatement()
                .delete(getTableName(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new DcpScopeMapping(this), operatorTranslator);
    }
}
