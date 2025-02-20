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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.TransferProcessStoreStatements;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;

/**
 * Maps fields of a {@link TransferProcess} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class TransferProcessMapping extends StatefulEntityMapping {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CREATED_TIMESTAMP = "createdAt";
    private static final String FIELD_DATAADDRESS = "dataAddress";
    private static final String FIELD_CONTENTDATAADDRESS = "contentDataAddress";
    private static final String FIELD_RESOURCE_MANIFEST = "resourceManifest";
    private static final String FIELD_PROVISIONED_RESOURCE_SET = "provisionedResourceSet";
    private static final String FIELD_DEPROVISIONED_RESOURCES = "deprovisionedResources";

    private static final String FIELD_PRIVATE_PROPERTIES = "privateProperties";
    private static final String FIELD_PENDING = "pending";
    private static final String FIELD_TRANSFER_TYPE = "transferType";
    private static final String FIELD_DATA_PLANE_ID = "dataPlaneId";

    private static final String FIELD_CORRELATION_ID = "correlationId";
    private static final String FIELD_COUNTER_PARTY_ADDRESS = "counterPartyAddress";
    private static final String FIELD_PROTOCOL = "protocol";
    private static final String FIELD_ASSET_ID = "assetId";
    private static final String FIELD_CONTRACT_ID = "contractId";
    private static final String FIELD_DATA_DESTINATION = "dataDestination";

    public TransferProcessMapping(TransferProcessStoreStatements statements) {
        super(statements, state -> TransferProcessStates.valueOf(state).code());
        add(FIELD_TYPE, statements.getTypeColumn());
        add(FIELD_CREATED_TIMESTAMP, statements.getCreatedAtColumn());
        add(FIELD_CORRELATION_ID, statements.getCorrelationIdColumn());
        add(FIELD_COUNTER_PARTY_ADDRESS, statements.getCounterPartyAddressColumn());
        add(FIELD_PROTOCOL, statements.getProtocolColumn());
        add(FIELD_ASSET_ID, statements.getAssetIdColumn());
        add(FIELD_CONTRACT_ID, statements.getContractIdColumn());
        add(FIELD_DATA_DESTINATION, new JsonFieldTranslator(statements.getDataDestinationColumn()));
        add(FIELD_DATAADDRESS, new JsonFieldTranslator(statements.getContentDataAddressColumn()));
        add(FIELD_CONTENTDATAADDRESS, new JsonFieldTranslator(statements.getContentDataAddressColumn()));
        add(FIELD_RESOURCE_MANIFEST, new ResourceManifestMapping());
        add(FIELD_PRIVATE_PROPERTIES, new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
        add(FIELD_PROVISIONED_RESOURCE_SET, new ProvisionedResourceSetMapping());
        // using the alias instead of the actual column name to avoid name clashes.
        add(FIELD_DEPROVISIONED_RESOURCES, new JsonFieldTranslator(PostgresDialectStatements.DEPROVISIONED_RESOURCES_ALIAS));
        add(FIELD_PENDING, statements.getPendingColumn());
        add(FIELD_TRANSFER_TYPE, statements.getTransferTypeColumn());
        add(FIELD_DATA_PLANE_ID, statements.getDataPlaneIdColumn());
    }
}
