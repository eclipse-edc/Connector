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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.BaseSqlDialect;
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and SQL table+column names required for the TransferProcessStore
 */
@ExtensionPoint
public interface TransferProcessStoreStatements extends LeaseStatements {

    String getInsertStatement();

    String getProcessIdForTransferIdTemplate();

    String getDeleteTransferProcessTemplate();

    String getNextForStateTemplate();

    String getUpdateTransferProcessTemplate();

    String getInsertDataRequestTemplate();

    String getSelectTemplate();

    String getUpdateDataRequestTemplate();

    default String getIdColumn() {
        return "transferprocess_id";
    }

    default String getTransferProcessTableName() {
        return "edc_transfer_process";
    }

    default String getStateColumn() {
        return "state";
    }

    default String getStateTimestampColumn() {
        return "state_time_stamp";
    }

    default String getTraceContextColumn() {
        return "trace_context";
    }

    default String getErrorDetailColumn() {
        return "error_detail";
    }

    default String getResourceManifestColumn() {
        return "resource_manifest";
    }

    default String getProvisionedResourcesetColumn() {
        return "provisioned_resource_set";
    }

    default String getTypeColumn() {
        return "type";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getContentDataAddressColumn() {
        return "content_data_address";
    }

    default String getAssetIdColumn() {
        return "asset_id";
    }

    default String getProtocolColumn() {
        return "protocol";
    }

    default String getConnectorIdColumn() {
        return "connector_id";
    }

    default String getConnectorAddressColumn() {
        return "connector_address";
    }

    default String getContractIdColumn() {
        return "contract_id";
    }

    default String getManagedResourcesColumn() {
        return "managed_resources";
    }

    default String getTransferTypeColumn() {
        return "transfer_type";
    }

    default String getProcessIdColumn() {
        return "process_id";
    }

    default String getPropertiesColumn() {
        return "transferprocess_properties";
    }

    default String getDataRequestPropertiesColumn() {
        return "properties";
    }

    default String getDataRequestTable() {
        return "edc_data_request";
    }

    default String getTransferProcessIdFkColumn() {
        return "transfer_process_id";
    }

    default String getDataDestinationColumn() {
        return "data_destination";
    }

    default String getStateCountColumn() {
        return "state_count";
    }

    default String getDataRequestIdColumn() {
        return "datarequest_id";
    }

    default String getDeprovisionedResourcesColumn() {
        return "deprovisioned_resources";
    }

    default String getClaimTokenColumn() {
        return "claim_token";
    }

    default String getFormatAsJsonOperator() {
        return BaseSqlDialect.getJsonCastOperator();
    }

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
