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

import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import static org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse.Status.OK;

/**
 * Implementation of the {@link ConsumerContractNegotiationManager}.
 */
public class ConsumerContractNegotiationManagerImpl implements ConsumerContractNegotiationManager {
    private ContractNegotiationStore store;

    public ConsumerContractNegotiationManagerImpl(ContractNegotiationStore store) {
        this.store = store;
    }

    @Override
    public NegotiationResponse initiate(ContractOffer offer) {
        var contractNegotiation = new ContractNegotiation();
        store.create(contractNegotiation);
        return new NegotiationResponse(OK, contractNegotiation);
    }

    @Override
    public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse confirmed(ClaimToken token, String negotiationId, ContractAgreement contract, String hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse declined(ClaimToken token, String negotiationId) {
        throw new UnsupportedOperationException();
    }
}
