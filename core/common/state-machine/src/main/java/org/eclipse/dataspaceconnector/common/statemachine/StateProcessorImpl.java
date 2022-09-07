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

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Permits processing states on the entities that are supplied by a specific supplier,
 * that could be a query on an entity store.
 * On every entity a process is applied.
 * A process is a function that returns a boolean that indicates if the entity has been processed or not in
 * the scope of the function.
 * The run method returns the processed state count, this is used by the state machine to decide
 * to apply the wait strategy or not.
 *
 * @param <T> the entity that is processed
 */
public class StateProcessorImpl<T> implements StateProcessor {

    private final Supplier<Collection<T>> entities;
    private final Function<T, Boolean> process;
    private final Predicate<Boolean> isProcessed = it -> it;

    public StateProcessorImpl(Supplier<Collection<T>> entitiesSupplier, Function<T, Boolean> process) {
        this.entities = entitiesSupplier;
        this.process = process;
    }

    @Override
    public Long process() {
        return entities.get().stream()
                .map(process)
                .filter(isProcessed)
                .count();
    }
}
