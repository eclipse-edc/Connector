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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;
import org.eclipse.edc.sql.translation.TranslationMapping;

import java.time.Clock;

import static java.lang.String.format;

/**
 * Concrete implementation of the {@link ContractNegotiationStatements} for Postgres. Uses a mapping tree
 * ({@link TranslationMapping} to generate queries.
 *
 * @see ContractNegotiationMapping
 * @see ContractAgreementMapping
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {


    public PostgresDialectStatements(LeaseStatements leaseStatements, Clock clock) {
        super(new PostgresqlOperatorTranslator(), leaseStatements, clock);
    }

    @Override
    public SqlQueryStatement createNegotiationsQuery(QuerySpec querySpec) {
        var selectStmt = getSelectNegotiationsTemplate();
        return new SqlQueryStatement(selectStmt, querySpec, new ContractNegotiationMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createNegotiationNextNotLeaseQuery(QuerySpec querySpec) {
        var queryTemplate = "%s LEFT JOIN %s l ON %s.%s = l.%s".formatted(getSelectNegotiationsTemplate(), leaseStatements.getLeaseTableName(), getContractNegotiationTable(), getIdColumn(), leaseStatements.getResourceIdColumn());
        return new SqlQueryStatement(queryTemplate, querySpec, new ContractNegotiationMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(), clock.millis(), getContractNegotiationTable());
    }

    private String getNotLeasedFilter() {
        return format("(l.%s IS NULL OR (? > (%s + %s) AND ? = l.%s))",
                leaseStatements.getResourceIdColumn(), leaseStatements.getLeasedAtColumn(), leaseStatements.getLeaseDurationColumn(), leaseStatements.getResourceKindColumn());
    }

    @Override
    public SqlQueryStatement createAgreementsQuery(QuerySpec querySpec) {
        var selectStmt = getSelectFromAgreementsTemplate();
        return new SqlQueryStatement(selectStmt, querySpec, new ContractAgreementMapping(this), operatorTranslator);
    }


    /**
     * Overridable operator to convert strings to JSON. For postgres, this is the "::json" operator
     */
    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
