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
import org.eclipse.dataspaceconnector.sql.translation.JsonFieldMapping;
import org.eclipse.dataspaceconnector.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement} onto
 * the corresponding SQL schema (= column names)
 */
class ContractAgreementMapping extends TranslationMapping {


    private static final String FIELD_ID = "id";
    private static final String FIELD_PROVIDER_AGENT_ID = "providerAgentId";
    private static final String FIELD_CONSUMER_AGENT_ID = "consumerAgentId";
    private static final String FIELD_CONTRACT_SIGNING_DATE = "contractSigningDate";
    private static final String FIELD_CONTRACT_START_DATE = "contractStartDate";
    private static final String FIELD_CONTRACT_END_DATE = "contractEndDate";
    private static final String FIELD_ASSET_ID = "assetId";
    private static final String FIELD_POLICY = "policy";

    ContractAgreementMapping(ContractNegotiationStatements statements) {
        add(FIELD_ID, statements.getContractAgreementIdColumn());
        add(FIELD_PROVIDER_AGENT_ID, statements.getProviderAgentColumn());
        add(FIELD_CONSUMER_AGENT_ID, statements.getConsumerAgentColumn());
        add(FIELD_CONTRACT_SIGNING_DATE, statements.getSigningDateColumn());
        add(FIELD_CONTRACT_START_DATE, statements.getStartDateColumn());
        add(FIELD_CONTRACT_END_DATE, statements.getEndDateColumn());
        add(FIELD_ASSET_ID, statements.getAssetIdColumn());
        add(FIELD_POLICY, new JsonFieldMapping("policy"));
    }
}
