/*
 *  Copyright (c) 2024 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial implementation
 *
 */

package org.eclipse.edc.catalog.crawler;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RecurringExecutionPlanTest {
    private static final Integer POLL_DELAY = 10;
    private final Monitor monitor = mock(Monitor.class);
    private RecurringExecutionPlan recurringExecutionPlan;

    @BeforeEach
    public void setUp() {
        var schedule = Duration.ofMillis(5);
        var initialDelay = Duration.ofMillis(1);
        recurringExecutionPlan = new RecurringExecutionPlan(schedule, initialDelay, monitor);
    }

    @Test
    public void runPlan_shouldExecuteAtLeastOnce() {
        AtomicInteger counter = new AtomicInteger();
        Runnable task = counter::incrementAndGet;

        recurringExecutionPlan.run(task);

        await().pollDelay(Duration.ofMillis(POLL_DELAY))
                .untilAsserted(() -> {
                    recurringExecutionPlan.stop();
                    assertThat(counter).hasPositiveValue();
                });
    }

    @Test
    public void stopPlanWithoutRun_shouldNotLogWarningsOrErrors() {
        recurringExecutionPlan.stop();

        verify(monitor, never()).warning(anyString());
        verify(monitor, never()).severe(anyString(), any(Throwable.class));
    }

    @Test
    public void runPlaneWithException_shouldLogError() {
        Runnable task = () -> {
            throw new RuntimeException("Test Exception");
        };

        recurringExecutionPlan.run(task);

        await().pollDelay(Duration.ofMillis(POLL_DELAY))
                .untilAsserted(() -> {
                    recurringExecutionPlan.stop();
                    verify(monitor, atLeastOnce())
                            .severe(anyString(), any(Throwable.class));
                });
    }

    @Test
    public void stopPlan_shouldPreventFurtherPlanExecution() {
        AtomicInteger counter = new AtomicInteger();
        Runnable task = counter::incrementAndGet;

        recurringExecutionPlan.run(task);

        await().pollDelay(Duration.ofMillis(POLL_DELAY))
                .until(() -> counter.get() > 0);

        recurringExecutionPlan.stop();

        int countAfterStop = counter.get();

        await().pollDelay(POLL_DELAY, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(counter.get()).isEqualTo(countAfterStop));
    }

}