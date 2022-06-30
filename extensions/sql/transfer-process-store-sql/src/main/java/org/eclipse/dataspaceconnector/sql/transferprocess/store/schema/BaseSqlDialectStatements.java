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

package org.eclipse.dataspaceconnector.sql.transferprocess.store.schema;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres.TransferProcessMapping;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

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
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?%s, ?, ?%s, ?%s, ?%s, ?, ?%s);",
                // keys
                getTransferProcessTableName(), getIdColumn(), getStateColumn(), getStateCountColumn(), getStateTimestampColumn(),
                getCreatedTimestampColumn(),
                getTraceContextColumn(), getErrorDetailColumn(), getResourceManifestColumn(),
                getProvisionedResourcesetColumn(), getContentDataAddressColumn(), getTypeColumn(), getDeprovisionedResourcesColumn(),
                // values
                getFormatAsJsonOperator(), getFormatAsJsonOperator(), getFormatAsJsonOperator(), getFormatAsJsonOperator(), getFormatAsJsonOperator());
    }

    @Override
    public String getProcessIdForTransferIdTemplate() {
        return format("SELECT * FROM %s WHERE %s.%s = (SELECT %s FROM %s WHERE %s.%s = ?);",
                getTransferProcessTableName(), getTransferProcessTableName(),
                getIdColumn(), getTransferProcessIdFkColumn(),
                getDataRequestTable(), getDataRequestTable(), getProcessIdColumn());
    }

    @Override
    public String getDeleteTransferProcessTemplate() {
        return format(DELETE_STATEMENT, getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getNextForStateTemplate() {
        return format("SELECT *, dr.%s as edc_data_request_id FROM %s LEFT OUTER JOIN %s dr ON %s.%s = dr.transfer_process_id " +
                        "WHERE %s=? " +
                        "AND (%s IS NULL OR %s IN (SELECT %s FROM %s WHERE (? > (%s + %s)))) " +
                        "ORDER BY %s ASC LIMIT ? ;",
                getDataRequestIdColumn(), getTransferProcessTableName(), getDataRequestTable(), getTransferProcessTableName(), getIdColumn(), getStateColumn(), getLeaseIdColumn(), getLeaseIdColumn(), getLeaseIdColumn(),
                getLeaseTableName(), getLeasedAtColumn(), getLeaseDurationColumn(), getStateTimestampColumn());
    }

    @Override
    public String getUpdateTransferProcessTemplate() {
        return format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?%s, %s=?, %s=?%s, %s=?%s, %s=?%s, %s=?%s WHERE %s=?",
                getTransferProcessTableName(), getStateColumn(), getStateCountColumn(), getStateTimestampColumn(),
                getTraceContextColumn(), getFormatAsJsonOperator(), getErrorDetailColumn(),
                getResourceManifestColumn(), getFormatAsJsonOperator(), getProvisionedResourcesetColumn(), getFormatAsJsonOperator(),
                getContentDataAddressColumn(), getFormatAsJsonOperator(), getDeprovisionedResourcesColumn(), getFormatAsJsonOperator(), getIdColumn());
    }

    @Override
    public String getInsertDataRequestTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?%s, ?%s, ?%s, ?, ?, ?);",
                getDataRequestTable(), getDataRequestIdColumn(), getProcessIdColumn(), getConnectorAddressColumn(), getConnectorIdColumn(),
                getAssetIdColumn(), getContractIdColumn(), getDataDestinationColumn(),
                getPropertiesColumn(),
                getTransferTypeColumn(), getTransferProcessIdFkColumn(), getProtocolColumn(), getManagedResourcesColumn(),
                getFormatAsJsonOperator(), getFormatAsJsonOperator(), getFormatAsJsonOperator());
    }

    @Override
    public String getSelectTemplate() {
        return String.format("SELECT *, edr.%s as edc_data_request_id FROM %s LEFT OUTER JOIN %s edr on %s.%s = edr.%s", getDataRequestIdColumn(),
                getTransferProcessTableName(), getDataRequestTable(), getTransferProcessTableName(), getIdColumn(), getProcessIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new TransferProcessMapping(this));
    }
}