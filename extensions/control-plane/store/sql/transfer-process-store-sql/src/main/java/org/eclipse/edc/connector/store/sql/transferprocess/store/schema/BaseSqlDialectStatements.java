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

package org.eclipse.edc.connector.store.sql.transferprocess.store.schema;

import org.eclipse.edc.connector.store.sql.transferprocess.store.schema.postgres.TransferProcessMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlExecuteStatement;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.ColumnEntry.standardColumn;

/**
 * Postgres-specific variants and implementations of the statements required for the TransferProcessStore
 */
public abstract class BaseSqlDialectStatements implements TransferProcessStoreStatements {

    private static final String DELETE_STATEMENT = "DELETE FROM %s WHERE %s = ?;";

    @Override
    public String getDeleteLeaseTemplate() {
        return format(DELETE_STATEMENT, getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s)" +
                "VALUES (?,?,?,?);", getLeaseTableName(), getLeaseIdColumn(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return format("UPDATE %s SET %s=? WHERE %s = ?;", getTransferProcessTableName(), getLeaseIdColumn(), getIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getInsertStatement() {
        return SqlExecuteStatement.newInstance(getFormatAsJsonOperator())
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .jsonColumn(getResourceManifestColumn())
                .jsonColumn(getProvisionedResourceSetColumn())
                .jsonColumn(getContentDataAddressColumn())
                .column(getTypeColumn())
                .jsonColumn(getDeprovisionedResourcesColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .jsonColumn(getCallbackAddressesColumn())
                .column(getPendingColumn())
                .insertInto(getTransferProcessTableName());
    }

    @Override
    public String getDeleteTransferProcessTemplate() {
        return format(DELETE_STATEMENT, getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getUpdateTransferProcessTemplate() {
        return SqlExecuteStatement.newInstance(getFormatAsJsonOperator())
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getUpdatedAtColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .jsonColumn(getResourceManifestColumn())
                .jsonColumn(getProvisionedResourceSetColumn())
                .jsonColumn(getContentDataAddressColumn())
                .jsonColumn(getDeprovisionedResourcesColumn())
                .jsonColumn(getCallbackAddressesColumn())
                .column(getPendingColumn())
                .update(getTransferProcessTableName(), standardColumn(getIdColumn()));
    }

    @Override
    public String getInsertDataRequestTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?%s, ?, ?);",
                getDataRequestTable(), getDataRequestIdColumn(), getProcessIdColumn(), getConnectorAddressColumn(),
                getConnectorIdColumn(), getAssetIdColumn(), getContractIdColumn(), getDataDestinationColumn(),
                getTransferProcessIdFkColumn(), getProtocolColumn(), getFormatAsJsonOperator());
    }

    @Override
    public String getSelectTemplate() {
        return format("SELECT *, edr.%s as edc_data_request_id FROM %s LEFT OUTER JOIN %s edr on %s.%s = edr.%s", getDataRequestIdColumn(),
                getTransferProcessTableName(), getDataRequestTable(), getTransferProcessTableName(), getIdColumn(), getProcessIdColumn());
    }

    @Override
    public String getUpdateDataRequestTemplate() {
        return format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?%s WHERE %s=?",
                getDataRequestTable(),
                getDataRequestIdColumn(), getProcessIdColumn(), getConnectorAddressColumn(), getProtocolColumn(), getConnectorIdColumn(), getAssetIdColumn(), getContractIdColumn(),
                getDataDestinationColumn(), getFormatAsJsonOperator(), getDataRequestIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new TransferProcessMapping(this));
    }

}
