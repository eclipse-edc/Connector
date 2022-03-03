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

package org.eclipse.dataspaceconnector.common.statemachine;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Handles a loop that processes entities continuously.
 * On every iteration it runs all the set processors sequentially,
 * applying a wait strategy in the case no entities are processed on the iteration.
 */
public class StateMachine {

    private final List<StateProcessor> processors = new ArrayList<>();
    private final ExecutorService executor;
    private final AtomicBoolean active = new AtomicBoolean();
    private final WaitStrategy waitStrategy;
    private final Monitor monitor;
    private final String name;
    private int shutdownTimeout = 10;

    private StateMachine(String name, Monitor monitor, WaitStrategy waitStrategy) {
        this.name = name;
        this.monitor = monitor;
        this.waitStrategy = waitStrategy;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            var thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("StateMachine-" + name);
            return thread;
        });
    }

    /**
     * Start the loop that will run processors until it's stopped
     *
     * @return a future that will complete when the loop stops
     */
    public Future<?> start() {
        active.set(true);
        return executor.submit(loop());
    }

    /**
     * Stop the loop gracefully
     *
     * @return a future that will complete when the loop is fully stopped.
     *         The content of the future will be true if stop happened before the timeout, false elsewhere.
     */
    public CompletableFuture<Boolean> stop() {
        active.set(false);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return executor.awaitTermination(shutdownTimeout, SECONDS);
            } catch (InterruptedException e) {
                monitor.severe(format("StateMachine [%s] await termination failed", name), e);
                return false;
            }
        });
    }

    /**
     * Tells if the loop is active and running
     *
     * @return true if it's active, false otherwise
     */
    public boolean isActive() {
        return active.get();
    }

    private Runnable loop() {
        return () -> {
            while (active.get()) {
                try {
                    var processed = processors.stream()
                            .mapToLong(StateProcessor::process)
                            .sum();

                    if (processed == 0) {
                        Thread.sleep(waitStrategy.waitForMillis());
                    }
                    waitStrategy.success();
                } catch (Error | InterruptedException e) {
                    active.set(false);
                    monitor.severe(format("StateMachine [%s] unrecoverable error", name), e);
                } catch (Throwable e) {
                    try {
                        monitor.severe(format("StateMachine [%s] error caught", name), e);
                        Thread.sleep(waitStrategy.retryInMillis());
                    } catch (InterruptedException ex) {
                        active.set(false);
                        monitor.severe(format("StateMachine [%s] unrecoverable error", name), e);
                    }
                }
            }
        };
    }

    public static class Builder {

        private final StateMachine loop;

        private Builder(String name, Monitor monitor, WaitStrategy waitStrategy) {
            this.loop = new StateMachine(name, monitor, waitStrategy);
        }

        public static Builder newInstance(String name, Monitor monitor, WaitStrategy waitStrategy) {
            return new Builder(name, monitor, waitStrategy);
        }

        public Builder processor(StateProcessor processor) {
            loop.processors.add(processor);
            return this;
        }

        public Builder shutdownTimeout(int seconds) {
            loop.shutdownTimeout = seconds;
            return this;
        }

        public StateMachine build() {
            return loop;
        }
    }
}
