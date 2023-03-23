/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - minor modifications
 *
 */

package org.eclipse.edc.connector.contract.spi.negotiation;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.response.StatusResult;

/**
 * Manages contract negotiations on the consumer participant.
 * <p>
 * All operations are idempotent.
 */
@ExtensionPoint
public interface ConsumerContractNegotiationManager extends ContractNegotiationManager {

    /**
     * Initiates a contract negotiation for the given provider offer. The offer will have been obtained from a previous contract offer request sent to the provider.
     */
    StatusResult<ContractNegotiation> initiate(ContractOfferRequest contractOffer);

    /**
     * The negotiation has been confirmed by the provider and the final contract received.
     */
    StatusResult<ContractNegotiation> confirmed(ClaimToken token, String negotiationId, ContractAgreement agreement, Policy policy);

    /**
     * The negotiation has been finalized by the provider.
     */
    StatusResult<ContractNegotiation> finalized(ClaimToken token, String negotiationId);
}
