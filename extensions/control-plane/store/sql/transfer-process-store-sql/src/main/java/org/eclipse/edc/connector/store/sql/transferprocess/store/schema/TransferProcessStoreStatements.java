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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and SQL table+column names required for the TransferProcessStore
 */
@ExtensionPoint
public interface TransferProcessStoreStatements extends StatefulEntityStatements, LeaseStatements {

    String getInsertStatement();

    String getDeleteTransferProcessTemplate();

    String getUpdateTransferProcessTemplate();

    String getInsertDataRequestTemplate();

    String getSelectTemplate();

    String getUpdateDataRequestTemplate();

    default String getTransferProcessTableName() {
        return "edc_transfer_process";
    }

    default String getIdColumn() {
        return "transferprocess_id";
    }

    default String getResourceManifestColumn() {
        return "resource_manifest";
    }

    default String getProvisionedResourceSetColumn() {
        return "provisioned_resource_set";
    }

    default String getTypeColumn() {
        return "type";
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

    default String getConnectorAddressColumn() {
        return "connector_address";
    }

    default String getContractIdColumn() {
        return "contract_id";
    }

    default String getProcessIdColumn() {
        return "process_id";
    }

    default String getPrivatePropertiesColumn() {
        return "private_properties";
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

    default String getDataRequestIdColumn() {
        return "datarequest_id";
    }

    default String getDeprovisionedResourcesColumn() {
        return "deprovisioned_resources";
    }

    default String getCallbackAddressesColumn() {
        return "callback_addresses";
    }

    default String getPendingColumn() {
        return "pending";
    }

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
