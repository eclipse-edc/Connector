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
 *       Mercedes-Benz Tech Innovation GmbH - connector id removal
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres.TransferProcessMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;

/**
 * Sql generic variants and implementations of the statements required for the TransferProcessStore
 */
public class BaseSqlDialectStatements implements TransferProcessStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;
    protected final LeaseStatements leaseStatements;
    protected final Clock clock;

    protected BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator, LeaseStatements leaseStatements, Clock clock) {
        this.operatorTranslator = operatorTranslator;
        this.leaseStatements = leaseStatements;
        this.clock = clock;
    }

    @Override
    public String getUpsertStatement() {
        return executeStatement()
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .jsonColumn(getContentDataAddressColumn())
                .column(getTypeColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .jsonColumn(getCallbackAddressesColumn())
                .column(getPendingColumn())
                .column(getTransferTypeColumn())
                .jsonColumn(getProtocolMessagesColumn())
                .column(getDataPlaneIdColumn())
                .column(getCorrelationIdColumn())
                .column(getCounterPartyAddressColumn())
                .column(getProtocolColumn())
                .column(getAssetIdColumn())
                .column(getContractIdColumn())
                .jsonColumn(getDataDestinationColumn())
                .column(getParticipantContextIdColumn())
                .jsonColumn(getDataplaneMetadataColumn())
                .column(getDataAddressAliasColumn())
                .upsertInto(getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getDeleteTransferProcessTemplate() {
        return executeStatement().delete(getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getTransferProcessTableName());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new TransferProcessMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        var queryTemplate = "%s LEFT JOIN %s l ON %s.%s = l.%s".formatted(getSelectTemplate(), leaseStatements.getLeaseTableName(), getTransferProcessTableName(), getIdColumn(), leaseStatements.getResourceIdColumn());
        return new SqlQueryStatement(queryTemplate, querySpec, new TransferProcessMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(), clock.millis(), getTransferProcessTableName());
    }

    private String getNotLeasedFilter() {
        return format("(l.%s IS NULL OR (? > (%s + %s) AND ? = l.%s))",
                leaseStatements.getResourceIdColumn(), leaseStatements.getLeasedAtColumn(), leaseStatements.getLeaseDurationColumn(), leaseStatements.getResourceKindColumn());
    }
}
