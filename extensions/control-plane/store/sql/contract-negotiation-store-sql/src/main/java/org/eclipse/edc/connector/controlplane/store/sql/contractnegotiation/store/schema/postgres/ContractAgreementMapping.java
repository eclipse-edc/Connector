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
public class ContractAgreementMapping extends TranslationMapping {

    public ContractAgreementMapping(ContractNegotiationStatements statements) {
        add("id", statements.getContractAgreementIdColumn());
        add("providerId", statements.getProviderAgentColumn());
        add("consumerId", statements.getConsumerAgentColumn());
        add("contractSigningDate", statements.getSigningDateColumn());
        add("assetId", statements.getAssetIdColumn());
        add("policy", new JsonFieldTranslator(statements.getPolicyColumn()));
        add("participantContextId", statements.getAgreementParticipantContextIdColumn());
        add("agreementId", statements.getContractAgreementContractIdColumn());
        add("claims", new JsonFieldTranslator(statements.getClaimsColumn()));
    }
}
