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
package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl implements ProviderContractNegotiationManager {

    @Override
    public NegotiationResponse declined(ClaimToken token, String negotiationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse requested(ClaimToken token, String correlationId, ContractOffer offer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer offer, String hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse consumerApproved(String negotiationId, String hash) {
        throw new UnsupportedOperationException();
    }
}
