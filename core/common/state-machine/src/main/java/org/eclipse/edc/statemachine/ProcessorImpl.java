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

import java.util.Collection;
import java.util.Objects;
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
 * An {@link Guard} can be registered, if its predicate is verified, the guard processor is executed instead of the standard one.
 *
 * @param <E> the entity that is processed
 */
public class ProcessorImpl<E> implements Processor {

    private final Supplier<Collection<E>> entities;
    private Function<E, Boolean> process;
    private Guard<E> guard = Guard.noop();

    private ProcessorImpl(Supplier<Collection<E>> entitiesSupplier) {
        entities = entitiesSupplier;
    }

    @Override
    public Long process() {
        return entities.get().stream()
                .map(entity -> {
                    if (guard.predicate().test(entity)) {
                        return guard.process().apply(entity);
                    } else {
                        return process.apply(entity);
                    }
                })
                .filter(isEqual(true))
                .count();
    }

    public static class Builder<E> {

        private final ProcessorImpl<E> processor;

        public Builder(Supplier<Collection<E>> entitiesSupplier) {
            processor = new ProcessorImpl<>(entitiesSupplier);
        }

        public static <E> Builder<E> newInstance(Supplier<Collection<E>> entitiesSupplier) {
            return new Builder<>(entitiesSupplier);
        }

        public Builder<E> process(Function<E, Boolean> process) {
            processor.process = process;
            return this;
        }

        public Builder<E> guard(Predicate<E> predicate, Function<E, Boolean> process) {
            processor.guard = new Guard<>(predicate, process);
            return this;
        }

        public ProcessorImpl<E> build() {
            Objects.requireNonNull(processor.process);

            return processor;
        }
    }

    private record Guard<E>(Predicate<E> predicate, Function<E, Boolean> process) {
        static <E> Guard<E> noop() {
            return new Guard<>(e -> false, e -> false);
        }
    }
}
