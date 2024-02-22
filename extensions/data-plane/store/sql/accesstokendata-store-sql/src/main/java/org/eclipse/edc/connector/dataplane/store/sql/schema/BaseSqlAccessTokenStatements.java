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

package org.eclipse.edc.connector.dataplane.store.sql.schema;

import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.AccessTokenDataMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public class BaseSqlAccessTokenStatements implements AccessTokenDataStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlAccessTokenStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .jsonColumn(getClaimTokenColumn())
                .jsonColumn(getDataAddressColumn())
                .insertInto(getTableName());
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
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new AccessTokenDataMapping(this), operatorTranslator);
    }
}
