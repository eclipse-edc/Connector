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

package org.eclipse.edc.connector.service.contractnegotiation;

import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.command.CancelNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.command.DeclineNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ContractNegotiationServiceImplTest {

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final ConsumerContractNegotiationManager consumerManager = mock(ConsumerContractNegotiationManager.class);
    private final ProviderContractNegotiationManager providerManager = mock(ProviderContractNegotiationManager.class);
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractNegotiationServiceImpl service = new ContractNegotiationServiceImpl(store, consumerManager, transactionContext);

    @Test
    void findById_filtersById() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.findById("negotiationId")).thenReturn(negotiation);

        var result = service.findbyId("negotiationId");

        assertThat(result).matches(it -> it.getId().equals("negotiationId"));
    }

    @Test
    void findById_returnsNullIfNotFound() {
        when(store.findById("negotiationId")).thenReturn(null);

        var result = service.findbyId("negotiationId");

        assertThat(result).isNull();
    }

    @Test
    void query_filtersBySpec() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.queryNegotiations(isA(QuerySpec.class))).thenReturn(Stream.of(negotiation));

        var result = service.query(QuerySpec.none());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(1).first().matches(it -> it.getId().equals("negotiationId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "contractAgreement.contractStartDate.begin=123455", //invalid path
            "contractOffers.policy.uid=some-id", //invalid path
            "contractOffers.policy.assetid=some-id", //wrong case
            "contractOffers.policy.=some-id", //incomplete path
    })
    void query_invalidFilter(String invalidFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(invalidFilter)
                .build();

        assertThat(service.query(query).failed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "contractAgreement.contractStartDate=123455",
            "contractAgreement.assetId=test-asset",
            "contractAgreement.policy.assignee=123455",
    })
    void query_validFilter(String validFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(validFilter)
                .build();
        service.query(query);
        verify(store).queryNegotiations(query);
    }

    @Test
    void getState_returnsStringRepresentation() {
        var negotiation = createContractNegotiationBuilder("negotiationId")
                .state(REQUESTED.code())
                .build();
        when(store.findById("negotiationId")).thenReturn(negotiation);

        var result = service.getState("negotiationId");

        assertThat(result).isEqualTo(REQUESTED.name());
    }

    @Test
    void getState_returnsNullIfNegotiationDoesNotExist() {
        when(store.findById("negotiationId")).thenReturn(null);

        var result = service.getState("negotiationId");

        assertThat(result).isNull();
    }

    @Test
    void getForNegotiation_filtersById() {
        var contractAgreement = createContractAgreement("agreementId");
        var negotiation = createContractNegotiation("negotiationId");
        negotiation.setContractAgreement(contractAgreement);

        when(store.findById("negotiationId")).thenReturn(negotiation);

        var result = service.getForNegotiation("negotiationId");

        assertThat(result).matches(it -> it.getId().equals("agreementId"));
        verify(store).findById(any());
        verifyNoMoreInteractions(store);
    }

    @Test
    void getForNegotiation_negotiationNotFound() {
        when(store.findById("negotiationId")).thenReturn(null);
        var result = service.getForNegotiation("negotiationId");
        assertThat(result).isNull();
        verify(store).findById(any());
        verifyNoMoreInteractions(store);
    }

    @Test
    void getForNegotiation_negotiationNoAgreement() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.findById("negotiationId")).thenReturn(negotiation);

        var result = service.getForNegotiation("negotiationId");

        assertThat(result).isNull();
        verify(store).findById(any());
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
        when(consumerManager.initiate(isA(ContractOfferRequest.class))).thenReturn(StatusResult.success(contractNegotiation));
        var request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .contractOffer(createContractOffer())
                .build();

        var result = service.initiateNegotiation(request);

        assertThat(result).matches(it -> it.getId().equals("negotiationId"));
    }

    @Test
    void cancel_shouldCancelNegotiationIfItCanBeCanceled() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.findById("negotiationId")).thenReturn(negotiation);

        var result = service.cancel("negotiationId");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).matches(it -> it.getId().equals("negotiationId"));
        verify(consumerManager).enqueueCommand(argThat(isCancelNegotiationCommandWithNegotiationId("negotiationId")));
    }

    @Test
    void cancel_shouldNotCancelNegationIfItDoesNotExist() {
        when(store.findById("negotiationId")).thenReturn(null);

        var result = service.cancel("negotiationId");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.reason()).isEqualTo(NOT_FOUND);
        verifyNoInteractions(consumerManager);
    }

    @Test
    void decline_shouldSucceedIfManagerIsBeingAbleToDeclineIt() {
        var negotiation = createContractNegotiationBuilder("negotiationId").state(REQUESTED.code()).build();
        when(store.findById("negotiationId")).thenReturn(negotiation);

        var result = service.decline("negotiationId");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).matches(it -> it.getId().equals("negotiationId"));
        verify(consumerManager).enqueueCommand(and(isA(DeclineNegotiationCommand.class), argThat(it -> "negotiationId".equals(it.getNegotiationId()))));
    }

    @Test
    void decline_shouldNotCancelNegationIfItDoesNotExist() {
        when(store.findById("negotiationId")).thenReturn(null);

        var result = service.decline("negotiationId");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.reason()).isEqualTo(NOT_FOUND);
        verifyNoInteractions(consumerManager);
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

    private ContractOffer createContractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("test-asset").build())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now())
                .build();
    }
}
