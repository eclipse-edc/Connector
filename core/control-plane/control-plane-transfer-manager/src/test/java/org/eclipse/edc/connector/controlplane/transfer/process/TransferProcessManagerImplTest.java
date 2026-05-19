/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.process;

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.response.StatusResult.success;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessManagerImplTest {

    private static final int RETRY_LIMIT = 1;
    private final TransferProcessStore transferProcessStore = mock();
    private final TransferProcessors transferProcessors = mock();
    private final TransferProcessPendingGuard pendingGuard = mock();
    private final Clock clock = Clock.systemUTC();

    private TransferProcessManagerImpl manager;

    @BeforeEach
    void setup() {
        when(transferProcessStore.save(any())).thenReturn(StoreResult.success());
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L));
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .transferProcessors(transferProcessors)
                .waitStrategy(() -> 10000L)
                .batchSize(10)
                .monitor(mock())
                .clock(clock)
                .store(transferProcessStore)
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .pendingGuard(pendingGuard)
                .build();
    }

    @Test
    void pendingGuard_shouldSetTheTransferPending_whenPendingGuardMatches() {
        when(pendingGuard.test(any())).thenReturn(true);
        var process = createTransferProcessBuilder(STARTING).build();
        when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessors.processStarting(any())).thenReturn(completedFuture(success()));

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(STARTING.code());
            assertThat(saved.isPending()).isTrue();
        });
    }

    private Criterion[] providerStateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending(), criterion("type", "=", PROVIDER.name())});
    }

    private TransferProcess.Builder createTransferProcessBuilder(org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates state) {
        return TransferProcess.Builder.newInstance()
                .type(PROVIDER)
                .id("test-process-" + UUID.randomUUID())
                .state(state.code())
                .correlationId(UUID.randomUUID().toString())
                .counterPartyAddress("http://an/address")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .participantContextId("participantContextId")
                .protocol("protocol");
    }
}
