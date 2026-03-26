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
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.STARTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Implementation of the {@link PolicyMonitorManager}.
 * <p>
 * Acts as a watchdog: on a fixed schedule it queries all active policy monitor entries and
 * evaluates their policies, terminating the associated transfer process when a policy is no
 * longer satisfied.
 */
public class PolicyMonitorManagerImpl implements PolicyMonitorManager {

    private final PolicyMonitorStore store;
    private final Monitor monitor;
    private final ExecutorInstrumentation executorInstrumentation;
    private final PolicyMonitor policyMonitor;
    private final PolicyMonitorConfiguration configuration;

    private ScheduledExecutorService scheduler;
    private final Clock clock;

    public PolicyMonitorManagerImpl(PolicyMonitor policyMonitor, PolicyMonitorConfiguration configuration,
                                    ExecutorInstrumentation executorInstrumentation, Monitor monitor,
                                    PolicyMonitorStore store, Clock clock) {
        this.policyMonitor = policyMonitor;
        this.configuration = configuration;
        this.executorInstrumentation = executorInstrumentation;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.store = store;
        this.clock = clock;
        scheduler = executorInstrumentation.instrument(
                Executors.newSingleThreadScheduledExecutor(r -> {
                    var thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setName("policy-monitor-watchdog");
                    return thread;
                }), "policy-monitor");
    }

    @Override
    public void start() {
        scheduler = executorInstrumentation.instrument(
                Executors.newSingleThreadScheduledExecutor(r -> {
                    var thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setName("policy-monitor-watchdog");
                    return thread;
                }), "policy-monitor");

        scheduler.schedule(this::checkPolicies, 0, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitor.severe("PolicyMonitorManager await termination failed", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void checkPolicies() {
        var count = 0;
        List<PolicyMonitorEntry> entries;
        do {
            entries = store.nextNotLeased(configuration.batchSize(), hasState(STARTED.code()), notYetProcessed());
            entries.forEach(policyMonitor::monitor);
            count += entries.size();
        } while (entries.size() >= configuration.batchSize());

        var period = configuration.period();
        scheduler.schedule(this::checkPolicies, period.getSeconds(), TimeUnit.SECONDS);
        monitor.debug("watchdog completed: %d entries evaluated. Next execution in %s".formatted(count, period));
    }

    private @NotNull Criterion notYetProcessed() {
        return criterion("updatedAt", "<", clock.millis() - configuration.period().toMillis());
    }
}
