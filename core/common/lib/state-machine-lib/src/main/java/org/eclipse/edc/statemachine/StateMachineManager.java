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

package org.eclipse.edc.statemachine;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Handles a loop that processes entities continuously.
 * On every iteration it runs all the set processors sequentially,
 * applying a wait strategy in the case no entities are processed on the iteration.
 */
public class StateMachineManager {

    private final List<Processor> processors = new ArrayList<>();
    private final List<Processor> startupProcessors = new ArrayList<>();
    private final ScheduledExecutorService executor;
    private final AtomicBoolean active = new AtomicBoolean();
    private final WaitStrategy waitStrategy;
    private final Monitor monitor;
    private final String name;
    private int shutdownTimeout = 10;

    private StateMachineManager(String name, Monitor monitor, ExecutorInstrumentation instrumentation, WaitStrategy waitStrategy) {
        this.name = name;
        this.monitor = monitor;
        this.waitStrategy = waitStrategy;
        executor = instrumentation.instrument(
                Executors.newSingleThreadScheduledExecutor(r -> {
                    var thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setName("StateMachineManager-" + name);
                    return thread;
                }), name);
    }

    /**
     * Start the loop that will run processors until it's stopped
     *
     * @return a future that will complete when the loop starts
     */
    public Future<?> start() {
        active.set(true);
        performStartupLogic();
        return scheduleNextIterationIn(0L);
    }

    /**
     * Stop the loop gracefully
     *
     * @return a future that will complete when the loop is fully stopped. The content of the future will be true if stop happened before the timeout, false elsewhere.
     */
    public CompletableFuture<Boolean> stop() {
        active.set(false);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return executor.awaitTermination(shutdownTimeout, SECONDS);
            } catch (InterruptedException e) {
                monitor.severe(format("StateMachineManager [%s] await termination failed", name), e);
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
            if (active.get()) {
                performLogic();
            }
        };
    }

    private void performStartupLogic() {
        for (var startupProcessor : startupProcessors) {
            try {
                long count;
                do {
                    count = startupProcessor.process();
                } while (count > 0);
            } catch (Throwable e) {
                monitor.severe(format("StateMachineManager [%s] startup error caught", name), e);
            }
        }
    }

    private void performLogic() {
        try {
            var processed = processors.stream()
                    .mapToLong(Processor::process)
                    .sum();

            waitStrategy.success();

            var delay = processed == 0 ? waitStrategy.waitForMillis() : 0;

            scheduleNextIterationIn(delay);
        } catch (Error e) {
            active.set(false);
            monitor.severe(format("StateMachineManager [%s] unrecoverable error", name), e);
        } catch (Throwable e) {
            monitor.severe(format("StateMachineManager [%s] error caught", name), e);
            scheduleNextIterationIn(waitStrategy.retryInMillis());
        }
    }

    @NotNull
    private Future<?> scheduleNextIterationIn(long delayMillis) {
        return executor.schedule(loop(), delayMillis, MILLISECONDS);
    }

    public static class Builder {

        private final StateMachineManager loop;

        private Builder(String name, Monitor monitor, ExecutorInstrumentation instrumentation, WaitStrategy waitStrategy) {
            loop = new StateMachineManager(name, monitor, instrumentation, waitStrategy);
        }

        public static Builder newInstance(String name, Monitor monitor, ExecutorInstrumentation instrumentation, WaitStrategy waitStrategy) {
            return new Builder(name, monitor, instrumentation, waitStrategy);
        }

        public Builder processor(Processor processor) {
            loop.processors.add(processor);
            return this;
        }

        public Builder shutdownTimeout(int seconds) {
            loop.shutdownTimeout = seconds;
            return this;
        }

        /**
         * Register a processor that will run once at startup before the regular processors.
         *
         * @param startupProcessor the processor.
         * @return the builder.
         */
        public Builder startupProcessor(Processor startupProcessor) {
            loop.startupProcessors.add(startupProcessor);
            return this;
        }

        public StateMachineManager build() {
            return loop;
        }
    }
}
