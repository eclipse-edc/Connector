/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;

public class BaseSqlDataPlaneInstanceStatements implements DataPlaneInstanceStatements {

    protected final SqlOperatorTranslator operatorTranslator;
    protected final LeaseStatements leaseStatements;
    protected final Clock clock;

    public BaseSqlDataPlaneInstanceStatements(SqlOperatorTranslator operatorTranslator, LeaseStatements leaseStatements, Clock clock) {
        this.operatorTranslator = operatorTranslator;
        this.leaseStatements = leaseStatements;
        this.clock = clock;
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public String getAllTemplate() {
        return format("SELECT * FROM %s", getDataPlaneInstanceTable());
    }

    @Override
    public String getUpsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .jsonColumn(getDataColumn())
                .upsertInto(getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getDataPlaneInstanceTable());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement()
                .delete(getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new DataPlaneInstanceMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        var queryTemplate = "%s LEFT JOIN %s l ON %s.%s = l.%s".formatted(getSelectTemplate(), leaseStatements.getLeaseTableName(), getDataPlaneInstanceTable(), getIdColumn(), leaseStatements.getResourceIdColumn());
        return new SqlQueryStatement(queryTemplate, querySpec, new DataPlaneInstanceMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(), clock.millis(), getDataPlaneInstanceTable());
    }

    private String getNotLeasedFilter() {
        return format("(l.%s IS NULL OR (? > (%s + %s) AND ? = l.%s))",
                leaseStatements.getResourceIdColumn(), leaseStatements.getLeasedAtColumn(), leaseStatements.getLeaseDurationColumn(), leaseStatements.getResourceKind());
    }

}
