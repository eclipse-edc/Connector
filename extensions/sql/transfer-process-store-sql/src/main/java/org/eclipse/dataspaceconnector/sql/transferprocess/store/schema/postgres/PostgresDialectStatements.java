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

package org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

/**
 * Postgres-specific variations of the SQL statements based on Postgres's ability to use JSON operators and -functions.
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public static final String DEPROVISIONED_RESOURCES_ALIAS = "dpr"; //must be different from column name to avoid ambiguities
    private static final String RESOURCES_ALIAS = "resources";
    private static final String DEFINITIONS_ALIAS = "definitions";

    @Override
    public String getFormatAsJsonOperator() {
        return "::json";
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        // if any criterion targets a JSON array field, we need to slightly adapt the FROM clause
        if (queryContains(querySpec, "resourceManifest.definitions")) {
            var select = getSelectFromJsonArrayTemplate(format("%s -> '%s'", getResourceManifestColumn(), "definitions"), DEFINITIONS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this));
        } else if (queryContains(querySpec, "provisionedResourceSet.resources")) {
            var select = getSelectFromJsonArrayTemplate(format("%s -> '%s'", getProvisionedResourcesetColumn(), "resources"), RESOURCES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this));
        } else if (queryContains(querySpec, "deprovisionedResources")) {
            var select = getSelectFromJsonArrayTemplate(format("%s", getDeprovisionedResourcesColumn()), DEPROVISIONED_RESOURCES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this));
        }
        return super.createQuery(querySpec);
    }

    private boolean queryContains(QuerySpec querySpec, String opLeft) {
        return querySpec.getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().toString().startsWith(opLeft));
    }

    /**
     * Creates a SELECT statement that targets a Postgres JSON array
     *
     * @param jsonPath The path to the array object, which is passed as parameter to the
     *         {@code json_array_elements()} function
     * @param aliasName the alias under which the JSON array is available, e.g. for WHERE clauses
     */
    private String getSelectFromJsonArrayTemplate(String jsonPath, String aliasName) {
        return format("%s, json_array_elements(%s) as %s", getSelectTemplate(), jsonPath, aliasName);
    }
}
