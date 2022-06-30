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

package org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.postgres;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.dataspaceconnector.sql.dialect.PostgresDialect;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

/**
 * Concrete implementation of the {@link ContractNegotiationStatements} for Postgres. Uses a mapping tree
 * ({@link org.eclipse.dataspaceconnector.sql.translation.TranslationMapping} to generate queries.
 *
 * @see ContractNegotiationMapping
 * @see ContractAgreementMapping
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    @Override
    public SqlQueryStatement createNegotiationsQuery(QuerySpec querySpec) {
        var selectStmt = getSelectNegotiationsTemplate();
        return new SqlQueryStatement(selectStmt, querySpec, new ContractNegotiationMapping(this));
    }

    @Override
    public SqlQueryStatement createAgreementsQuery(QuerySpec querySpec) {
        var selectStmt = getSelectFromAgreementsTemplate();
        return new SqlQueryStatement(selectStmt, querySpec, new ContractAgreementMapping(this));
    }

    /**
     * Overridable operator to convert strings to JSON. For postgres, this is the "::json" operator
     */
    @Override
    protected String getFormatJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
