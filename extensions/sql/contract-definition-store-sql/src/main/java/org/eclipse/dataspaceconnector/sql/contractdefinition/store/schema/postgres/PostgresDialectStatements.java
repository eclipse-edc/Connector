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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.dialect.PostgresDialect;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

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
        if (querySpec.containsAnyLeftOperand("selectorExpression.criteria")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), format("%s -> '%s'", getSelectorExpressionColumn(), "criteria"), "criteria");
            return new SqlQueryStatement(select, querySpec, new ContractDefinitionMapping(this));
        }
        return super.createQuery(querySpec);
    }


}
