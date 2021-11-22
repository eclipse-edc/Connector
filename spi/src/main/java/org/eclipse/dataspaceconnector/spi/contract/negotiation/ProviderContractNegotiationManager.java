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
 *
 */
package org.eclipse.dataspaceconnector.spi.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

/**
 * Manages contract negotiations on the provider.
 *
 * All operations are idempotent.
 */
public interface ProviderContractNegotiationManager extends ContractNegotiationManager {

    /**
     * A contract negotiation has been requested by the consumer represented with the given claims.
     */
    NegotiationResponse requested(ClaimToken token, String correlationId, ContractOffer offer);

    /**
     * A new offer was made by the consumer represented by the claim token.
     */
    NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer offer, String hash);

    /**
     * Confirms a contract negotiation after it has been approved by both counter-parties. A final contract will be sent to the consumer.
     */
    NegotiationResponse consumerApproved(String negotiationId, String hash);

}
