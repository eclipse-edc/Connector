/*
 *  Copyright (c) 2021 - 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       Cofinity-X - add participantId to DataspaceProfileContext
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.response.StatusResult.success;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderContractNegotiationManagerImplTest {

    private static final int RETRY_LIMIT = 1;
    private final ContractNegotiationStore store = mock();
    private final NegotiationProcessors negotiationProcessors = mock();
    private final ContractNegotiationPendingGuard pendingGuard = mock();
    private ProviderContractNegotiationManagerImpl manager;

    @BeforeEach
    void setUp() {
        when(store.save(any())).thenReturn(StoreResult.success());
        manager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .negotiationProcessors(negotiationProcessors)
                .monitor(mock())
                .store(store)
                .entityRetryProcessConfiguration(new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L)))
                .pendingGuard(pendingGuard)
                .build();
    }

    @Test
    void pendingGuard_shouldSetTheNegotiationPending_whenPendingGuardMatches() {
        when(pendingGuard.test(any())).thenReturn(true);
        var negotiation = contractNegotiationBuilder().state(AGREEING.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(AGREEING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(negotiationProcessors.processAgreeing(any())).thenReturn(completedFuture(success()));

        manager.start();

        await().untilAsserted(() -> {
            verify(pendingGuard).test(any());
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(AGREEING.code());
            assertThat(saved.isPending()).isTrue();
        });
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending(), new Criterion("type", "=", "PROVIDER")});
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(ContractNegotiation.Type.PROVIDER)
                .correlationId("processId")
                .counterPartyId("connectorId")
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .state(400)
                .participantContextId("participantContextId")
                .stateTimestamp(Instant.now().toEpochMilli());
    }
}
