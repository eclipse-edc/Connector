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
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

/**
 * Contains Postgres-specific SQL statements
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {
    @Override
    public String getFormatAsJsonOperator() {
        return "::json";
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        // if any criterion targets a JSON array field, we need to slightly adapt the FROM clause
        if (querySpec.getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().toString().startsWith("selectorExpression.criteria"))) {
            var select = getSelectFromJsonArrayTemplate(format("%s -> '%s'", getSelectorExpressionColumn(), "criteria"), "criteria");
            return new SqlQueryStatement(select, querySpec, new ContractDefinitionMapping(this));
        }
        return super.createQuery(querySpec);
    }

    /**
     * Creates a SELECT statement that targets a Postgres JSON array
     *
     * @param jsonPath The path to the array object, which is passed as parameter to the
     *         {@code json_array_elements()} function
     * @param aliasName the alias under which the JSON array is available, e.g. for WHERE clauses
     */
    private String getSelectFromJsonArrayTemplate(String jsonPath, String aliasName) {
        return format("SELECT * FROM %s, json_array_elements(%s) as %s", getContractDefinitionTable(), jsonPath, aliasName);
    }
}
