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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and SQL table+column names required for the TransferProcessStore
 */
@ExtensionPoint
public interface TransferProcessStoreStatements extends StatefulEntityStatements, SqlStatements {

    String getUpsertStatement();

    String getDeleteTransferProcessTemplate();

    String getSelectTemplate();

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

    default String getCounterPartyAddressColumn() {
        return "counter_party_address";
    }

    default String getTransferTypeColumn() {
        return "transfer_type";
    }

    default String getDataPlaneIdColumn() {
        return "data_plane_id";
    }

    default String getContractIdColumn() {
        return "contract_id";
    }

    default String getPrivatePropertiesColumn() {
        return "private_properties";
    }

    default String getDataDestinationColumn() {
        return "data_destination";
    }

    default String getCorrelationIdColumn() {
        return "correlation_id";
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

    default String getProtocolMessagesColumn() {
        return "protocol_messages";
    }

    SqlQueryStatement createQuery(QuerySpec querySpec);

    SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec);

}
