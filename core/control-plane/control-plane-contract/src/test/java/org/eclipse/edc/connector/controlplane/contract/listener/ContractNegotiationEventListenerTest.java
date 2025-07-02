/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.contract.listener;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationVerified;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContractNegotiationEventListenerTest {

    private final EventRouter router = mock();
    private final ContractNegotiationEventListener listener = new ContractNegotiationEventListener(router);

    @Test
    void initiated_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.initiated(negotiation);

        var eventPayload = ContractNegotiationInitiated.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);
    }

    @Test
    void requested_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.requested(negotiation);

        var eventPayload = ContractNegotiationRequested.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);

    }

    @Test
    void offered_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.offered(negotiation);

        var eventPayload = ContractNegotiationOffered.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);
    }

    @Test
    void accepted_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.accepted(negotiation);

        var eventPayload = ContractNegotiationAccepted.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);

    }

    @Test
    void terminated_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.terminated(negotiation);

        var eventPayload = ContractNegotiationTerminated.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);

    }

    @Test
    void agreed_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.agreed(negotiation);

        var eventPayload = ContractNegotiationAgreed.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);

    }

    @Test
    void verified_shouldDispatchEvent() {
        var negotiation = getNegotiation("id");

        listener.verified(negotiation);

        var eventPayload = ContractNegotiationVerified.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .build();

        assertEvent(eventPayload);

    }

    @Test
    void finalized_shouldDispatchEvent() {
        var agreement = ContractAgreement.Builder.newInstance()
                .id("id")
                .policy(Policy.Builder.newInstance().build())
                .assetId("assetId")
                .consumerId("consumer")
                .providerId("provider")
                .build();
        var negotiation = getNegotiationBuilder("id")
                .contractAgreement(agreement)
                .build();

        listener.finalized(negotiation);

        var eventPayload = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .counterPartyId(negotiation.getCounterPartyId())
                .contractAgreement(agreement)
                .build();

        assertEvent(eventPayload);

    }

    private ContractNegotiation getNegotiation(String id) {
        return getNegotiationBuilder(id).build();
    }

    private ContractNegotiation.Builder getNegotiationBuilder(String id) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .protocol("test-protocol")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .events(Set.of("test"))
                        .transactional(true)
                        .build()))
                .state(0);
    }

    private void assertEvent(ContractNegotiationEvent eventPayload) {
        var eventCaptor = ArgumentCaptor.forClass(ContractNegotiationEvent.class);
        verify(router).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).usingRecursiveComparison().isEqualTo(eventPayload);
    }
}
