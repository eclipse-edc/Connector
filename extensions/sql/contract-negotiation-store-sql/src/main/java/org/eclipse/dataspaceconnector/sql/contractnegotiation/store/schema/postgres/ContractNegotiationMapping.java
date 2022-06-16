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

package org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.postgres;

import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.dataspaceconnector.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation}
 * onto the corresponding SQL schema (= column names)
 */
class ContractNegotiationMapping extends TranslationMapping {
    private static final String FIELD_ID = "id";
    private static final String FIELD_CORRELATION_ID = "correlationId";
    private static final String FIELD_COUNTER_PARTY_ID = "counterPartyId";
    private static final String FIELD_COUNTERPARTY_ADDRESS = "counterPartyAddress";
    private static final String FIELD_PROTOCOL = "protocol";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_STATECOUNT = "stateCount";
    private static final String FIELD_STATETIMESTAMP = "stateTimestamp";
    private static final String FIELD_ERRORDETAIL = "errorDetail";
    private static final String FIELD_CONTRACT_AGREEMENT = "contractAgreement";
    private static final String FIELD_TRACECONTEXT = "traceContext";


    ContractNegotiationMapping(ContractNegotiationStatements statements) {
        // cannot use Map.of(), because that only accepts 10 pairs
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CORRELATION_ID, statements.getCorrelationIdColumn());
        add(FIELD_COUNTER_PARTY_ID, statements.getCounterPartyIdColumn());
        add(FIELD_COUNTERPARTY_ADDRESS, statements.getCounterPartyAddressColumn());
        add(FIELD_PROTOCOL, statements.getProtocolColumn());
        add(FIELD_TYPE, statements.getTypeColumn());
        add(FIELD_STATE, statements.getStateColumn());
        add(FIELD_STATECOUNT, statements.getStateCountColumn());

        add(FIELD_STATETIMESTAMP, statements.getStateTimestampColumn());
        add(FIELD_ERRORDETAIL, statements.getErrorDetailColumn());

        fieldMap.put(FIELD_CONTRACT_AGREEMENT, new ContractAgreementMapping(statements));
        add(FIELD_TRACECONTEXT, statements.getTraceContextColumn());
    }


}
