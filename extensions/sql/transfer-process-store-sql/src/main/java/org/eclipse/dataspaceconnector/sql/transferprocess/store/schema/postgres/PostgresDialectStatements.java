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
import org.eclipse.dataspaceconnector.sql.dialect.PostgresDialect;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Postgres-specific variations of the SQL statements based on Postgres's ability to use JSON operators and -functions.
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public static final String DEPROVISIONED_RESOURCES_ALIAS = "dpr"; //must be different from column name to avoid ambiguities
    private static final String RESOURCES_ALIAS = "resources";
    private static final String DEFINITIONS_ALIAS = "definitions";

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        // if any criterion targets a JSON array field, we need to slightly adapt the FROM clause
        if (querySpec.containsAnyLeftOperand("resourceManifest.definitions")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), format("%s -> '%s'", getResourceManifestColumn(), "definitions"), DEFINITIONS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this));
        } else if (querySpec.containsAnyLeftOperand("provisionedResourceSet.resources")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), format("%s -> '%s'", getProvisionedResourcesetColumn(), "resources"), RESOURCES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this));
        } else if (querySpec.containsAnyLeftOperand("deprovisionedResources")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), format("%s", getDeprovisionedResourcesColumn()), DEPROVISIONED_RESOURCES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this));
        }
        return super.createQuery(querySpec);
    }
}
