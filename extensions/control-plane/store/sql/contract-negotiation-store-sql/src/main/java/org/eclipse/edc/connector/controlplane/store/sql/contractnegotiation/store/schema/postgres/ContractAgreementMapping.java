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

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link ContractAgreement} onto
 * the corresponding SQL schema (= column names)
 */
class ContractAgreementMapping extends TranslationMapping {


    public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final String FIELD_ID = "id";
    private static final String FIELD_PROVIDER_AGENT_ID = "providerId";
    private static final String FIELD_CONSUMER_AGENT_ID = "consumerId";
    private static final String FIELD_CONTRACT_SIGNING_DATE = "contractSigningDate";
    private static final String FIELD_ASSET_ID = "assetId";
    private static final String FIELD_POLICY = "policy";

    ContractAgreementMapping(ContractNegotiationStatements statements) {
        add(FIELD_ID, statements.getContractAgreementIdColumn());
        add(FIELD_PROVIDER_AGENT_ID, statements.getProviderAgentColumn());
        add(FIELD_CONSUMER_AGENT_ID, statements.getConsumerAgentColumn());
        add(FIELD_CONTRACT_SIGNING_DATE, statements.getSigningDateColumn());
        add(FIELD_ASSET_ID, statements.getAssetIdColumn());
        add(FIELD_POLICY, new JsonFieldTranslator("policy"));
        add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getAgreementParticipantContextIdColumn());
    }
}
