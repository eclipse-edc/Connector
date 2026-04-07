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

    public TransferProcessMapping(TransferProcessStoreStatements statements) {
        super(statements, state -> TransferProcessStates.valueOf(state).code());
        add("type", statements.getTypeColumn());
        add("correlationId", statements.getCorrelationIdColumn());
        add("counterPartyAddress", statements.getCounterPartyAddressColumn());
        add("protocol", statements.getProtocolColumn());
        add("assetId", statements.getAssetIdColumn());
        add("contractId", statements.getContractIdColumn());
        add("dataDestination", new JsonFieldTranslator(statements.getDataDestinationColumn()));
        add("dataAddress", new JsonFieldTranslator(statements.getContentDataAddressColumn()));
        add("contentDataAddress", new JsonFieldTranslator(statements.getContentDataAddressColumn()));
        add("privateProperties", new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
        add("pending", statements.getPendingColumn());
        add("transferType", statements.getTransferTypeColumn());
        add("dataPlaneId", statements.getDataPlaneIdColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("claims", new JsonFieldTranslator(statements.getClaimsColumn()));
    }
}
