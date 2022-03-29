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

import org.eclipse.dataspaceconnector.sql.lease.LeaseStatements;

/**
 * Statement templates and SQL table+column names required for the TransferProcessStore
 */
public interface TransferProcessStoreStatements extends LeaseStatements {

    String getInsertStatement();

    String getFindByIdStatement();

    String getProcessIdForTransferIdTemplate();

    String getDeleteTransferProcessTemplate();

    String getNextForStateTemplate();

    String getUpdateTransferProcessTemplate();

    String getInsertDataRequestTemplate();

    String getQueryStatement();


    default String getIdColumn() {
        return "id";
    }

    default String getTableName() {
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

    default String getAssetIdColumn() {
        return "asset_id";
    }

    default String getProtocolColumn() {
        return "protocol";
    }

    default String getDestinationColumn() {
        return "data_destination";
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
        return "properties";
    }

    default String getDataRequestTable() {
        return "edc_data_request";
    }

    default String getTransferProcessIdColumn() {
        return "transfer_process_id";
    }

    default String getDataDestinationColumn() {
        return "data_destination";
    }

    default String getStateCountColumn() {
        return "state_count";
    }


}
