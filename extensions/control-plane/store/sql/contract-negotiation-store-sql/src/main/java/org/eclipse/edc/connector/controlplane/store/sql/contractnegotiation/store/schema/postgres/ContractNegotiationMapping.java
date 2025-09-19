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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;

/**
 * Maps fields of a {@link ContractNegotiation}
 * onto the corresponding SQL schema (= column names)
 */
class ContractNegotiationMapping extends StatefulEntityMapping {
    public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final String FIELD_CORRELATION_ID = "correlationId";
    private static final String FIELD_COUNTER_PARTY_ID = "counterPartyId";
    private static final String FIELD_COUNTERPARTY_ADDRESS = "counterPartyAddress";
    private static final String FIELD_PROTOCOL = "protocol";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CONTRACT_AGREEMENT = "contractAgreement";
    private static final String FIELD_TRACECONTEXT = "traceContext";
    private static final String FIELD_PENDING = "pending";

    ContractNegotiationMapping(ContractNegotiationStatements statements) {
        super(statements, state -> ContractNegotiationStates.valueOf(state).code());
        add(FIELD_CORRELATION_ID, statements.getCorrelationIdColumn());
        add(FIELD_COUNTER_PARTY_ID, statements.getCounterPartyIdColumn());
        add(FIELD_COUNTERPARTY_ADDRESS, statements.getCounterPartyAddressColumn());
        add(FIELD_PROTOCOL, statements.getProtocolColumn());
        add(FIELD_TYPE, statements.getTypeColumn());
        add(FIELD_PENDING, statements.getPendingColumn());
        add(FIELD_CONTRACT_AGREEMENT, new ContractAgreementMapping(statements));
        add(FIELD_TRACECONTEXT, statements.getTraceContextColumn());
        add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getParticipantContextIdColumn());
    }


}
