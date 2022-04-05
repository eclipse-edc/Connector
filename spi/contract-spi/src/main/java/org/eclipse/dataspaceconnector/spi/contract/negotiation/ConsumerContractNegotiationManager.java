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

package org.eclipse.dataspaceconnector.spi.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

/**
 * Manages contract negotiations on the consumer participant.
 * <p>
 * All operations are idempotent.
 */
public interface ConsumerContractNegotiationManager extends ContractNegotiationManager {

    /**
     * Initiates a contract negotiation for the given provider offer. The offer will have been obtained from a previous contract offer request sent to the provider.
     */
    NegotiationResult initiate(ContractOfferRequest contractOffer);

    /**
     * An offer was received from the provider.
     */
    NegotiationResult offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash);

    /**
     * The negotiation has been confirmed by the provider and the final contract received.
     */
    NegotiationResult confirmed(ClaimToken token, String negotiationId, ContractAgreement contract, String hash);
}
