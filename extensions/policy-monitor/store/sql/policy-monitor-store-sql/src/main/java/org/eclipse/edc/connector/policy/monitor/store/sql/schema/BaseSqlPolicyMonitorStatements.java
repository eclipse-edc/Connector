/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.policy.monitor.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;

public class BaseSqlPolicyMonitorStatements implements PolicyMonitorStatements {

    protected final SqlOperatorTranslator operatorTranslator;
    protected final LeaseStatements leaseStatements;
    protected final Clock clock;

    public BaseSqlPolicyMonitorStatements(SqlOperatorTranslator operatorTranslator, LeaseStatements leaseStatements, Clock clock) {
        this.operatorTranslator = operatorTranslator;
        this.leaseStatements = leaseStatements;
        this.clock = clock;
    }

    @Override
    public String getUpsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .column(getContractIdColumn())
                .upsertInto(getPolicyMonitorTable(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getPolicyMonitorTable());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new PolicyMonitorMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        var queryTemplate = "%s LEFT JOIN %s l ON %s.%s = l.%s".formatted(getSelectTemplate(), leaseStatements.getLeaseTableName(), getPolicyMonitorTable(), getIdColumn(), leaseStatements.getResourceIdColumn());
        return new SqlQueryStatement(queryTemplate, querySpec, new PolicyMonitorMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(), clock.millis(), getPolicyMonitorTable());
    }

    private String getNotLeasedFilter() {
        return format("(l.%s IS NULL OR (? > (%s + %s) AND ? = l.%s))",
                leaseStatements.getResourceIdColumn(), leaseStatements.getLeasedAtColumn(), leaseStatements.getLeaseDurationColumn(), leaseStatements.getResourceKind());
    }

}
