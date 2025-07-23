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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Provides database-related constants, such as column names, table names and statement templates. Methods to compose
 * statements must be overridden by implementors.
 */
public interface ContractNegotiationStatements extends StatefulEntityStatements, LeaseStatements {
    String getFindTemplate();

    String getFindContractAgreementTemplate();

    String getUpdateNegotiationTemplate();

    String getInsertNegotiationTemplate();

    String getDeleteTemplate();

    String getUpsertNegotiationTemplate();

    String getSelectFromAgreementsTemplate();

    String getSelectNegotiationsTemplate();

    String getUpsertAgreementTemplate();

    default String getContractNegotiationTable() {
        return "edc_contract_negotiation";
    }

    default String getContractAgreementTable() {
        return "edc_contract_agreement";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getCounterPartyIdColumn() {
        return "counterparty_id";
    }

    default String getCounterPartyAddressColumn() {
        return "counterparty_address";
    }

    default String getProtocolColumn() {
        return "protocol";
    }

    default String getCorrelationIdColumn() {
        return "correlation_id";
    }

    default String getProviderAgentColumn() {
        return "provider_agent_id";
    }

    default String getConsumerAgentColumn() {
        return "consumer_agent_id";
    }

    default String getSigningDateColumn() {
        return "signing_date";
    }

    default String getContractAgreementIdColumn() {
        return "agr_id";
    }

    default String getAssetIdColumn() {
        return "asset_id";
    }

    default String getPolicyColumn() {
        return "policy";
    }

    default String getContractAgreementIdFkColumn() {
        return "agreement_id";
    }

    default String getStateTimestampColumn() {
        return "state_timestamp";
    }

    default String getContractOffersColumn() {
        return "contract_offers";
    }

    default String getCallbackAddressesColumn() {
        return "callback_addresses";
    }

    default String getTypeColumn() {
        return "type";
    }

    default String getPendingColumn() {
        return "pending";
    }

    default String getProtocolMessagesColumn() {
        return "protocol_messages";
    }

    SqlQueryStatement createNegotiationsQuery(QuerySpec querySpec);

    SqlQueryStatement createAgreementsQuery(QuerySpec querySpec);
}
