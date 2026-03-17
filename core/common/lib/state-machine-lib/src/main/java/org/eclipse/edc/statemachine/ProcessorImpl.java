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

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.function.Predicate.isEqual;

/**
 * Describes the processing flow applied by a state machine. The entities are provided by a supplier.
 * A process is a function that returns a boolean that indicates if the entity has been processed or not in
 * the scope of the function.
 * The run method returns the processed state count, this is used by the state machine to decide
 * to apply the wait strategy or not.
 * <p>
 * Additional features:
 * - An {@link Guard} can be registered, if its predicate is verified, the guard processor is executed instead of the standard one.
 * - A onNotProcessed listener can be registered, that will be called on every entity that has not been processed.
 *
 * @param <E> the entity that is processed
 */
public class ProcessorImpl<E extends StatefulEntity<E>> implements Processor {

    private final Supplier<Collection<E>> entities;
    private final EntityRetryProcessConfiguration configuration;
    private final Clock clock;
    private final Monitor monitor;
    private Function<E, CompletableFuture<StatusResult<Void>>> process;
    private Guard<E> guard = Guard.noop();
    private Consumer<E> onNotProcessed = e -> {};

    private ProcessorImpl(Supplier<Collection<E>> entitiesSupplier, EntityRetryProcessConfiguration entityRetryProcessConfiguration, Clock clock, Monitor monitor) {
        entities = entitiesSupplier;
        configuration = entityRetryProcessConfiguration;
        this.clock = clock;
        this.monitor = monitor;
    }

    @Override
    public Long process() {
        return entities.get().stream()
                .map(this::process)
                .filter(isEqual(true))
                .count();
    }

    private @NotNull Boolean process(E entity) {
        if (isRetry(entity)) {
            var delay = delayMillis(entity);
            if (delay > 0) {
                monitor.debug(String.format("Entity %s %s retry #%d will not be attempted before %d ms.", entity.getId(), entity.getClass().getSimpleName(), entity.getStateCount() - 1, delay));
                onNotProcessed.accept(entity);
                return false;
            } else {
                monitor.debug(String.format("Entity %s %s retry #%d of %d.", entity.getId(), entity.getClass().getSimpleName(), entity.getStateCount() - 1, configuration.retryLimit()));
            }
        }

        var actualProcess = guard.predicate().test(entity) ? guard.process() : process;
        actualProcess.apply(entity);
        return true;
    }

    private boolean isRetry(E entity) {
        return entity.getStateCount() - 1 > 0;
    }

    private long delayMillis(E entity) {
        var delayStrategy = configuration.delayStrategySupplier().get();

        // Set the WaitStrategy to have observed <retryCount> previous failures.
        // This is relevant for stateful strategies such as exponential wait.
        delayStrategy.failures(entity.getStateCount() - 1);

        // Get the delay time following the number of failures.
        var waitMillis = delayStrategy.retryInMillis();

        return entity.getStateTimestamp() + waitMillis - clock.millis();
    }

    public static class Builder<E extends StatefulEntity<E>> {

        private final ProcessorImpl<E> processor;

        private Builder(Supplier<Collection<E>> entitiesSupplier, EntityRetryProcessConfiguration entityRetryProcessConfiguration,
                        Clock clock, Monitor monitor) {
            processor = new ProcessorImpl<>(entitiesSupplier, entityRetryProcessConfiguration, clock, monitor);
        }

        public static <E extends StatefulEntity<E>> Builder<E> newInstance(Supplier<Collection<E>> entitiesSupplier,
                                                                           EntityRetryProcessConfiguration entityRetryProcessConfiguration,
                                                                           Clock clock, Monitor monitor) {
            return new Builder<>(entitiesSupplier, entityRetryProcessConfiguration, clock, monitor);
        }

        public Builder<E> process(Function<E, CompletableFuture<StatusResult<Void>>> process) {
            processor.process = process;
            return this;
        }

        public Builder<E> guard(Predicate<E> predicate, Function<E, CompletableFuture<StatusResult<Void>>> process) {
            processor.guard = new Guard<>(predicate, process);
            return this;
        }

        /**
         * Defines a listener that will invoke for every entity that won't be processed.
         *
         * @param onNotProcessed the listener.
         * @return the builder.
         */
        public Builder<E> onNotProcessed(Consumer<E> onNotProcessed) {
            processor.onNotProcessed = onNotProcessed;
            return this;
        }

        public ProcessorImpl<E> build() {
            Objects.requireNonNull(processor.process);

            return processor;
        }
    }

    private record Guard<E>(Predicate<E> predicate, Function<E, CompletableFuture<StatusResult<Void>>> process) {
        static <E> Guard<E> noop() {
            return new Guard<>(e -> false, e -> CompletableFuture.completedFuture(StatusResult.success()));
        }
    }
}
