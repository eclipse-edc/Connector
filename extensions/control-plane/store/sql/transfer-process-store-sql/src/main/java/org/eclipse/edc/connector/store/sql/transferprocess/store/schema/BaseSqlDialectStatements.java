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

package org.eclipse.edc.connector.store.sql.transferprocess.store.schema;

import org.eclipse.edc.connector.store.sql.transferprocess.store.schema.postgres.TransferProcessMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

/**
 * Postgres-specific variants and implementations of the statements required for the TransferProcessStore
 */
public abstract class BaseSqlDialectStatements implements TransferProcessStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    protected BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getDeleteLeaseTemplate() {
        return executeStatement().delete(getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .insertInto(getLeaseTableName());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .update(getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getInsertStatement() {
        return executeStatement()
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
                .column(getTransferTypeColumn())
                .jsonColumn(getProtocolMessagesColumn())
                .column(getDataPlaneIdColumn())
                .insertInto(getTransferProcessTableName());
    }

    @Override
    public String getDeleteTransferProcessTemplate() {
        return executeStatement().delete(getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getUpdateTransferProcessTemplate() {
        return executeStatement()
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
                .column(getTransferTypeColumn())
                .jsonColumn(getProtocolMessagesColumn())
                .column(getDataPlaneIdColumn())
                .update(getTransferProcessTableName(), getIdColumn());
    }

    @Override
    public String getInsertDataRequestTemplate() {
        return executeStatement()
                .column(getDataRequestIdColumn())
                .column(getProcessIdColumn())
                .column(getConnectorAddressColumn())
                .column(getAssetIdColumn())
                .column(getContractIdColumn())
                .jsonColumn(getDataDestinationColumn())
                .column(getTransferProcessIdFkColumn())
                .column(getProtocolColumn())
                .insertInto(getDataRequestTable());
    }

    @Override
    public String getSelectTemplate() {
        return format("SELECT *, edr.%s as edc_data_request_id FROM %s LEFT OUTER JOIN %s edr on %s.%s = edr.%s", getDataRequestIdColumn(),
                getTransferProcessTableName(), getDataRequestTable(), getTransferProcessTableName(), getIdColumn(), getProcessIdColumn());
    }

    @Override
    public String getUpdateDataRequestTemplate() {
        return executeStatement()
                .column(getDataRequestIdColumn())
                .column(getProcessIdColumn())
                .column(getConnectorAddressColumn())
                .column(getProtocolColumn())
                .column(getAssetIdColumn())
                .column(getContractIdColumn())
                .jsonColumn(getDataDestinationColumn())
                .update(getDataRequestTable(), getDataRequestIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new TransferProcessMapping(this), operatorTranslator);
    }

}
