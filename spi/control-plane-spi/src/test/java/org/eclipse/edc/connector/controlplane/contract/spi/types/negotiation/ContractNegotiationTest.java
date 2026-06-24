/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;

public class ContractNegotiationTest {

    @Test
    void verify_consumerComplete() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
        negotiation.transitionAccepting();
        negotiation.transitionAccepted();
        negotiation.transitionAgreed();
        negotiation.transitionVerifying();
        negotiation.transitionVerified();
        negotiation.transitionFinalized();
    }

    @Test
    void verify_consumerRequestedAgreed() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionAgreed();
    }

    @Test
    void verify_consumerRequestedTerminated() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_consumerOfferedTerminated() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_consumerAcceptedTerminated() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
        negotiation.transitionAccepting();
        negotiation.transitionAccepted();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_consumerAgreedTerminated() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
        negotiation.transitionAccepting();
        negotiation.transitionAccepted();
        negotiation.transitionAgreed();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_consumerVerifiedTerminated() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
        negotiation.transitionAccepting();
        negotiation.transitionAccepted();
        negotiation.transitionAgreed();
        negotiation.transitionVerifying();
        negotiation.transitionVerified();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_consumerFinalizedTerminal() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionAgreed();
        negotiation.transitionVerifying();
        negotiation.transitionVerified();
        negotiation.transitionFinalized();
        assertThatThrownBy(negotiation::transitionTerminated).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verify_consumerCounterOffer() {
        var negotiation = createNegotiation(CONSUMER);
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
        negotiation.transitionRequesting();
        negotiation.transitionRequested();
        negotiation.transitionOffered();
    }

    @Test
    void verify_providerRequetedAgreedComplete() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionAgreeing();
        negotiation.transitionAgreed();
        negotiation.transitionVerified();
        negotiation.transitionFinalizing();
        negotiation.transitionFinalized();
        assertThatThrownBy(negotiation::transitionTerminated).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verify_providerComplete() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionOffering();
        negotiation.transitionOffered();
        negotiation.transitionAccepted();
        negotiation.transitionAgreeing();
        negotiation.transitionAgreed();
        negotiation.transitionVerified();
        negotiation.transitionFinalizing();
        negotiation.transitionFinalized();
        assertThatThrownBy(negotiation::transitionTerminated).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verify_providerRequestedTerminated() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_providerOfferedTerminated() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionOffering();
        negotiation.transitionOffered();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_providerAcceptedTerminated() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_providerAgreedTerminated() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionOffering();
        negotiation.transitionOffered();
        negotiation.transitionAccepted();
        negotiation.transitionAgreeing();
        negotiation.transitionAgreed();
        negotiation.transitionTerminated();
    }

    @Test
    void verify_providerVerifiedTerminated() {
        var negotiation = createNegotiation(PROVIDER);
        negotiation.transitionRequested();
        negotiation.transitionOffering();
        negotiation.transitionOffered();
        negotiation.transitionAccepted();
        negotiation.transitionAgreeing();
        negotiation.transitionAgreed();
        negotiation.transitionVerified();
        negotiation.transitionTerminated();
    }

    private ContractNegotiation createNegotiation(ContractNegotiation.Type type) {
        return ContractNegotiation.Builder.newInstance()
                .counterPartyId("counterpartyId")
                .counterPartyAddress("https://test.com")
                .type(type)
                .protocol("test")
                .build();
    }
}
