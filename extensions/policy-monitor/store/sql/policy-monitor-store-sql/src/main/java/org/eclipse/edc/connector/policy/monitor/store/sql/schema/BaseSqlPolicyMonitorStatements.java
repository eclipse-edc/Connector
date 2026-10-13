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
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlPolicyMonitorStatements implements PolicyMonitorStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlPolicyMonitorStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
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
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .upsertInto(getPolicyMonitorTable(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getPolicyMonitorTable());
    }

    @Override
    public String getAcquireLeaseTemplate() {
        return format("UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ? AND %s",
                getPolicyMonitorTable(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn(),
                getIdColumn(), getNotLeasedFilter());
    }

    @Override
    public String getBreakLeaseTemplate() {
        return format("UPDATE %s SET %s = NULL, %s = NULL, %s = NULL WHERE %s = ? AND (%s IS NULL OR %s = ? OR (%s + %s) < ?)",
                getPolicyMonitorTable(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn(), getIdColumn(),
                getLeasedByColumn(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new PolicyMonitorMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec, long now) {
        return createQuery(querySpec)
                .addWhereClause(getNotLeasedFilter(), now)
                .forUpdate(true);
    }

    private String getNotLeasedFilter() {
        return format("(%s IS NULL OR (%s + %s) < ?)", getLeasedAtColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

}
