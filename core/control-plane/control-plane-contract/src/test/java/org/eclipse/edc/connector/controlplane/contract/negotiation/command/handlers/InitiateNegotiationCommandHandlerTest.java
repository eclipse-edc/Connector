/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.InitiateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InitiateNegotiationCommandHandlerTest {

    private final ContractNegotiationStore store = mock();
    private final ContractNegotiationListener listener = mock();
    private final ContractNegotiationObservable observable = new ContractNegotiationObservableImpl();
    private final InitiateNegotiationCommandHandler handler = new InitiateNegotiationCommandHandler(store, observable, mock(), mock());

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void shouldSaveNewNegotiationInInitialState() {
        when(store.save(any())).thenReturn(StoreResult.success());
        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participantContextId")
                .identity("participantId")
                .build();
        var request = ContractRequest.Builder.newInstance()
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .build()))
                .build();

        var result = handler.handle(new InitiateNegotiationCommand(participantContext, request));

        assertThat(result).isSucceeded().isNotNull().isInstanceOfSatisfying(ContractNegotiation.class, negotiation -> {
            assertThat(negotiation.getState()).isEqualTo(INITIAL.code());
            assertThat(negotiation.getCounterPartyId()).isEqualTo("providerId");
            assertThat(negotiation.getCounterPartyAddress()).isEqualTo(request.getCounterPartyAddress());
            assertThat(negotiation.getProtocol()).isEqualTo(request.getProtocol());
            assertThat(negotiation.getCorrelationId()).isNull();
            assertThat(negotiation.getContractOffers()).hasSize(1);
            assertThat(negotiation.getLastContractOffer()).isEqualTo(contractOffer());
            assertThat(negotiation.getCallbackAddresses()).hasSize(1);

            verify(store).save(negotiation);
        });

        verify(listener).initiated(any());
    }

    @Test
    void shouldFail_whenStorageFails() {
        when(store.save(any())).thenReturn(StoreResult.generalError("error"));
        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participantContextId")
                .identity("participantId")
                .build();
        var request = ContractRequest.Builder.newInstance()
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .build()))
                .build();

        var result = handler.handle(new InitiateNegotiationCommand(participantContext, request));

        assertThat(result).isFailed();
        verifyNoInteractions(listener);
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance().id("id:assetId:random")
                .policy(Policy.Builder.newInstance().assigner("providerId").build())
                .assetId("assetId")
                .build();
    }


}
