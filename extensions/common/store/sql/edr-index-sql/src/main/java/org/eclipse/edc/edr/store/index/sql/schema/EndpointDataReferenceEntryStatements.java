/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.edr.store.index.sql.schema;

import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines all statements that are needed for the {@link EndpointDataReferenceEntry} store
 */
public interface EndpointDataReferenceEntryStatements extends SqlStatements {
    default String getTransferProcessIdColumn() {
        return "transfer_process_id";
    }

    default String getAgreementIdColumn() {
        return "agreement_id";
    }

    default String getAssetIdColumn() {
        return "asset_id";
    }

    default String getProviderIdColumn() {
        return "provider_id";
    }

    default String getContractNegotiationIdColumn() {
        return "contract_negotiation_id";
    }

    default String getEdrEntryTable() {
        return "edc_edr_entry";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }


    String getDeleteByIdTemplate();

    String getFindByTemplate();

    String getInsertTemplate();

    String getCountTemplate();

    String getUpdateTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}
