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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;
import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Postgres-specific variations of the SQL statements based on Postgres's ability to use JSON operators and -functions.
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public static final String DEPROVISIONED_RESOURCES_ALIAS = "dpr"; //must be different from column name to avoid ambiguities
    private static final String RESOURCES_ALIAS = "resources";
    private static final String DEFINITIONS_ALIAS = "definitions";


    public PostgresDialectStatements(LeaseStatements leaseStatements, Clock clock) {
        super(new PostgresqlOperatorTranslator(), leaseStatements, clock);
    }

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        // if any criterion targets a JSON array field, we need to slightly adapt the FROM clause
        if (querySpec.containsAnyLeftOperand("resourceManifest.definitions")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), format("%s -> '%s'", getResourceManifestColumn(), "definitions"), DEFINITIONS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this), operatorTranslator);
        } else if (querySpec.containsAnyLeftOperand("provisionedResourceSet.resources")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), format("%s -> '%s'", getProvisionedResourceSetColumn(), "resources"), RESOURCES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this), operatorTranslator);
        } else if (querySpec.containsAnyLeftOperand("deprovisionedResources")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), format("%s", getDeprovisionedResourcesColumn()), DEPROVISIONED_RESOURCES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new TransferProcessMapping(this), operatorTranslator);
        }
        return super.createQuery(querySpec);
    }
}
