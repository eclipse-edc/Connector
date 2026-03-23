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

package org.eclipse.edc.connector.policy.monitor.manager;

import org.eclipse.edc.connector.policy.monitor.PolicyMonitorConfiguration;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolicyMonitorManagerImplTest {

    public static final int BATCH_SIZE = 2;
    private final PolicyMonitorStore store = mock();
    private final PolicyMonitor policyMonitor = mock();
    private final Clock clock = mock();
    private final ExecutorInstrumentation executorInstrumentation = ExecutorInstrumentation.noop();
    private final PolicyMonitorManagerImpl manager = new PolicyMonitorManagerImpl(policyMonitor,
            new PolicyMonitorConfiguration(BATCH_SIZE, Duration.ofSeconds(4)), executorInstrumentation, mock(), store, clock);

    @Test
    void shouldStopProcessing_whenNoItemsAreReturned() {
        when(store.nextNotLeased(anyInt(), any(), any())).thenReturn(emptyList());

        manager.start();

        await().pollDelay(1, SECONDS).untilAsserted(() -> {
            verify(store, only()).nextNotLeased(eq(BATCH_SIZE), any(), any());
            verifyNoInteractions(policyMonitor);
        });
    }

    @Test
    void shouldProcessUntilItemsAreReturned() {
        when(store.nextNotLeased(anyInt(), any(), any()))
                .thenReturn(List.of(entry(), entry()))
                .thenReturn(List.of(entry()));

        manager.start();

        await().pollDelay(1, SECONDS).untilAsserted(() -> {
            verify(store, times(2)).nextNotLeased(eq(BATCH_SIZE), any(), any());
            verify(policyMonitor, times(3)).monitor(any());
        });
    }

    private PolicyMonitorEntry entry() {
        return PolicyMonitorEntry.Builder.newInstance().build();
    }
}
