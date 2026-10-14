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

/**
 * Sql generic variants and implementations of the statements required for the TransferProcessStore
 */
public class BaseSqlDialectStatements implements TransferProcessStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;
    protected final Clock clock;

    protected BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator, Clock clock) {
        this.operatorTranslator = operatorTranslator;
        this.clock = clock;
    }

    /**
     * Constructor kept for backward compatibility, the lease is now stored on the transfer process row.
     *
     * @deprecated use {@link #BaseSqlDialectStatements(SqlOperatorTranslator, Clock)}.
     */
    @Deprecated(since = "1.0.0")
    protected BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator, LeaseStatements leaseStatements, Clock clock) {
        this(operatorTranslator, clock);
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
                .column(getParticipantContextIdColumn())
                .jsonColumn(getDataplaneMetadataColumn())
                .column(getDataAddressAliasColumn())
                .jsonColumn(getClaimsColumn())
                .column(getDataAddressOwnerColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .upsertInto(getTransferProcessTableName(), getIdColumn(), getLeaseOwnedOrFreeFilter(getTransferProcessTableName()));
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
        return createQuery(querySpec)
                .addWhereClause(getNotLeasedFilter(getTransferProcessTableName()), clock.millis())
                .forUpdate(true);
    }
}
