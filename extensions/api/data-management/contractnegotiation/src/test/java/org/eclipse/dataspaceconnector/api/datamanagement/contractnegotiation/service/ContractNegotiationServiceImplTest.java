/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service;

import org.eclipse.dataspaceconnector.contract.negotiation.command.commands.CancelNegotiationCommand;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ContractNegotiationServiceImplTest {

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final ConsumerContractNegotiationManager manager = mock(ConsumerContractNegotiationManager.class);
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractNegotiationServiceImpl service = new ContractNegotiationServiceImpl(store, manager, transactionContext);

    @Test
    void findById_filtersById() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.find("negotiationId")).thenReturn(negotiation);

        var result = service.findbyId("negotiationId");

        assertThat(result).matches(it -> it.getId().equals("negotiationId"));
    }

    @Test
    void findById_returnsNullIfNotFound() {
        when(store.find("negotiationId")).thenReturn(null);

        var result = service.findbyId("negotiationId");

        assertThat(result).isNull();
    }

    @Test
    void query_filtersBySpec() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.queryNegotiations(isA(QuerySpec.class))).thenReturn(Stream.of(negotiation));

        var result = service.query(QuerySpec.none());

        assertThat(result).hasSize(1).first().matches(it -> it.getId().equals("negotiationId"));
    }

    @Test
    void getState_returnsStringRepresentation() {
        var negotiation = createContractNegotiationBuilder("negotiationId")
                .state(REQUESTED.code())
                .build();
        when(store.find("negotiationId")).thenReturn(negotiation);

        var result = service.getState("negotiationId");

        assertThat(result).isEqualTo("REQUESTED");
    }

    @Test
    void getState_returnsNullIfNegotiationDoesNotExist() {
        when(store.find("negotiationId")).thenReturn(null);

        var result = service.getState("negotiationId");

        assertThat(result).isEqualTo(null);
    }

    @Test
    void getForNegotiation_filtersById() {
        var contractAgreement = createContractAgreement("agreementId");
        var negotiation = createContractNegotiation("negotiationId");
        negotiation.setContractAgreement(contractAgreement);

        when(store.find("negotiationId")).thenReturn(negotiation);

        var result = service.getForNegotiation("negotiationId");

        assertThat(result).matches(it -> it.getId().equals("agreementId"));
        verify(store).find(any());
        verifyNoMoreInteractions(store);
    }

    @Test
    void getForNegotiation_negotiationNotFound() {
        when(store.find("negotiationId")).thenReturn(null);
        var result = service.getForNegotiation("negotiationId");
        assertThat(result).isNull();
        verify(store).find(any());
        verifyNoMoreInteractions(store);
    }

    @Test
    void getForNegotiation_negotiationNoAgreement() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.find("negotiationId")).thenReturn(negotiation);

        var result = service.getForNegotiation("negotiationId");

        assertThat(result).isNull();
        verify(store).find(any());
        verifyNoMoreInteractions(store);
    }

    @Test
    void getForNegotiation_returnsNullIfNotFound() {
        when(store.findContractAgreement("agreementId")).thenReturn(null);

        var result = service.getForNegotiation("agreementId");

        assertThat(result).isNull();
    }

    @Test
    void initiateNegotiation_callsManager() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(manager.initiate(isA(ContractOfferRequest.class))).thenReturn(StatusResult.success(contractNegotiation));
        var request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .contractOffer(ContractOffer.Builder.newInstance().id(UUID.randomUUID().toString()).policy(Policy.Builder.newInstance().build()).build())
                .build();

        var result = service.initiateNegotiation(request);

        assertThat(result).matches(it -> it.getId().equals("negotiationId"));
    }

    @Test
    void cancel_shouldCancelNegotiationIfItCanBeCanceled() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.find("negotiationId")).thenReturn(negotiation);

        var result = service.cancel("negotiationId");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).matches(it -> it.getId().equals("negotiationId"));
        verify(manager).enqueueCommand(argThat(isCancelNegotiationCommandWithNegotiationId("negotiationId")));
    }

    @Test
    void cancel_shouldNotCancelNegationIfItDoesNotExist() {
        when(store.find("negotiationId")).thenReturn(null);

        var result = service.cancel("negotiationId");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.reason()).isEqualTo(NOT_FOUND);
        verifyNoInteractions(manager);
    }

    @Test
    void decline_shouldSucceedIfManagerIsBeingAbleToDeclineIt() {
        var negotiation = createContractNegotiation("negotiationId");
        when(manager.declined(any(), any())).thenReturn(StatusResult.success(negotiation));

        var result = service.decline("negotiationId");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).matches(it -> it.getId().equals("negotiationId"));
        verify(manager).declined(isA(ClaimToken.class), eq("negotiationId"));
    }

    @Test
    void decline_shouldFailIfManagerIsNotBeingAbleToDeclineIt() {
        when(store.find("negotiationId")).thenReturn(null);
        when(manager.declined(any(), any())).thenReturn(StatusResult.failure(FATAL_ERROR));

        var result = service.decline("negotiationId");

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(CONFLICT);
        verify(manager).declined(isA(ClaimToken.class), eq("negotiationId"));
    }

    @Test
    void decline_shouldFailIfManagerIsNotBeingAbleToDeclineItAndThrowsException() {
        when(store.find("negotiationId")).thenReturn(null);
        when(manager.declined(any(), any())).thenThrow(new IllegalStateException("Cannot transition from a state to another"));

        var result = service.decline("negotiationId");

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(CONFLICT);
        verify(manager).declined(isA(ClaimToken.class), eq("negotiationId"));
    }

    @NotNull
    private ArgumentMatcher<ContractNegotiationCommand> isCancelNegotiationCommandWithNegotiationId(String negotiationId) {
        return it -> ((CancelNegotiationCommand) it).getNegotiationId().equals(negotiationId);
    }

    private ContractNegotiation createContractNegotiation(String negotiationId) {
        return createContractNegotiationBuilder(negotiationId)
                .build();
    }

    private ContractAgreement createContractAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerAgentId(UUID.randomUUID().toString())
                .consumerAgentId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol");
    }
}