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
 *       Fraunhofer Institute for Software and Systems Engineering - extended parameter definition
 *
 */

package org.eclipse.dataspaceconnector.spi.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

/**
 * Manages contract negotiations on the provider.
 * <p>
 * All operations are idempotent.
 */
public interface ProviderContractNegotiationManager extends ContractNegotiationManager {

    /**
     * A contract negotiation has been requested by the consumer represented with the given claims.
     */
    StatusResult<ContractNegotiation> requested(ClaimToken token, ContractOfferRequest request);

    /**
     * A new offer was made by the consumer represented by the claim token.
     */
    StatusResult<ContractNegotiation> offerReceived(ClaimToken token, String correlationId, ContractOffer offer, String hash);

    /**
     * Confirms a contract negotiation after it has been approved by both counter-parties. A final contract will be sent to the consumer.
     */
    StatusResult<ContractNegotiation> consumerApproved(ClaimToken token, String correlationId, ContractAgreement agreement, String hash);

}
