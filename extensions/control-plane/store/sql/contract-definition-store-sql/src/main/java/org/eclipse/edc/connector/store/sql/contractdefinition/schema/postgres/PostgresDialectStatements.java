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

package org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres;

import org.eclipse.edc.connector.store.sql.contractdefinition.schema.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Contains Postgres-specific SQL statements
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        // if any criterion targets a JSON array field, we need to slightly adapt the FROM clause
        if (querySpec.containsAnyLeftOperand("assetsSelector.")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), getAssetsSelectorColumn(), getAssetsSelectorAlias());
            return new SqlQueryStatement(select, querySpec, new ContractDefinitionMapping(this));
        }
        return super.createQuery(querySpec);
    }


}
