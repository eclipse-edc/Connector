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

package org.eclipse.dataspaceconnector.sql.transferprocess.store;

import static java.lang.String.format;

/**
 * Postgres-specific variants and implementations of the statements required for the TransferProcessStore
 */
public class PostgresStatements implements TransferProcessStoreStatements {
    @Override
    public String getDeleteLeaseTemplate() {
        return format("DELETE FROM %s WHERE %s=?", getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s)" +
                "VALUES (?,?,?,?);", getLeaseTableName(), getLeaseIdColumn(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return format("UPDATE %s SET %s=? WHERE %s = ?;", getTableName(), getLeaseIdColumn(), getIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getTableName(), getIdColumn());
    }

    @Override
    public String getInsertStatement() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                getTableName(), getIdColumn(), getStateColumn(), getStateCountColumn(), getStateTimestampColumn(),
                getCreatedTimestampColumn(),
                getTraceContextColumn(), getErrorDetailColumn(), getResourceManifestColumn(),
                getProvisionedResourcesetColumn(), getContentDataAddressColumn(), getTypeColumn());
    }

    @Override
    public String getFindByIdStatement() {
        return "SELECT *, dr.id as edc_data_request_id FROM edc_transfer_process LEFT OUTER JOIN edc_data_request dr ON edc_transfer_process.id = dr.transfer_process_id WHERE edc_transfer_process.id=?";
    }

    @Override
    public String getProcessIdForTransferIdTemplate() {
        return format("SELECT * FROM %s WHERE %s.%s = (SELECT %s FROM %s WHERE %s.%s = ?);",
                getTableName(), getTableName(), getIdColumn(), getTransferProcessIdColumn(), getDataRequestTable(), getDataRequestTable(), getProcessIdColumn());
    }

    @Override
    public String getDeleteTransferProcessTemplate() {
        return format("DELETE FROM %s WHERE id = ?;", getTableName());
    }

    @Override
    public String getNextForStateTemplate() {
        return format("SELECT *, dr.id as edc_data_request_id FROM %s LEFT OUTER JOIN edc_data_request dr ON edc_transfer_process.id = dr.transfer_process_id " +
                        "WHERE %s=? " +
                        "AND (%s IS NULL OR %s IN (SELECT %s FROM %s WHERE (? > (%s + %s)))) " +
                        "ORDER BY %s ASC LIMIT ? ;", getTableName(), getStateColumn(), getLeaseIdColumn(), getLeaseIdColumn(), getLeaseIdColumn(),
                getLeaseTableName(), getLeasedAtColumn(), getLeaseDurationColumn(), getStateTimestampColumn());
    }

    @Override
    public String getUpdateTransferProcessTemplate() {
        return format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?", getTableName(), getStateColumn(),
                getStateCountColumn(), getStateTimestampColumn(), getTraceContextColumn(), getErrorDetailColumn(),
                getResourceManifestColumn(), getProvisionedResourcesetColumn(), getContentDataAddressColumn(),
                getIdColumn());
    }

    @Override
    public String getInsertDataRequestTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                getDataRequestTable(), getIdColumn(), getProcessIdColumn(), getConnectorAddressColumn(), getConnectorIdColumn(),
                getAssetIdColumn(), getContractIdColumn(), getDataDestinationColumn(), getPropertiesColumn(),
                getTransferTypeColumn(), getTransferProcessIdColumn(), getProtocolColumn(), getManagedResourcesColumn());
    }

    @Override
    public String getQueryStatement() {
        return "SELECT *, dr.id as edc_data_request_id FROM edc_transfer_process LEFT OUTER JOIN edc_data_request dr ON edc_transfer_process.id = dr.transfer_process_id LIMIT ? OFFSET ?";
    }
}