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

package org.eclipse.edc.catalog.cache.sql.schema.postgres;

import org.eclipse.edc.catalog.cache.sql.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public static final String DATASETS_ALIAS = "datasets";
    public static final String DATA_SERVICES_ALIAS = "dataServices";

    public PostgresDialectStatements() {
        super(new PostgresqlOperatorTranslator());
    }

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }


    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        if (querySpec.containsAnyLeftOperand("datasets")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), format("%s -> '%s'", getCatalogColumn(), "datasets"), DATASETS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new FederatedCatalogMapping(this), operatorTranslator);
        } else if (querySpec.containsAnyLeftOperand("dataServices")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), format("%s -> '%s'", getCatalogColumn(), "dataServices"), DATA_SERVICES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new FederatedCatalogMapping(this), operatorTranslator);
        } else {
            return super.createQuery(querySpec);
        }
    }
}
