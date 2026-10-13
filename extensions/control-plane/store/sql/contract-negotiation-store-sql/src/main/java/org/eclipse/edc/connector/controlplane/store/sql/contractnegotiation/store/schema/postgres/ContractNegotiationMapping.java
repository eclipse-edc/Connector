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
import org.eclipse.edc.sql.translation.JsonArrayFieldTranslator;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;

/**
 * Maps fields of a {@link ContractNegotiation}
 * onto the corresponding SQL schema (= column names)
 */
public class ContractNegotiationMapping extends StatefulEntityMapping {

    public ContractNegotiationMapping(ContractNegotiationStatements statements) {
        super(statements, state -> ContractNegotiationStates.valueOf(state).code());
        add("correlationId", statements.getCorrelationIdColumn());
        add("counterPartyId", statements.getCounterPartyIdColumn());
        add("counterPartyAddress", statements.getCounterPartyAddressColumn());
        add("protocol", statements.getProtocolColumn());
        add("type", statements.getTypeColumn());
        add("pending", statements.getPendingColumn());
        add("contractAgreement", new ContractAgreementMapping(statements));
        add("contractOffers", new JsonArrayFieldTranslator(statements.getContractOffersColumn()));
        add("traceContext", statements.getTraceContextColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("claims", new JsonFieldTranslator(statements.getNegotiationClaimsColumn()));
    }


}
