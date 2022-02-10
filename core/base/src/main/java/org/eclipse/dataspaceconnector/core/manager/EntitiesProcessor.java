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

package org.eclipse.dataspaceconnector.core.manager;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility class that permits to process entities that are supplied by a specific supplier, that could be a query on
 * an entity store. On every entity a process is applied.
 * A process is a function that returns a boolean that indicates if the entity has processed or not in the scope of the
 * function.
 * The doProcess method returns the count of how many entities have been processed, this is used by the state machine
 * loop to decide to apply the wait strategy or not.
 *
 * @param <T> the entity that is processed
 */
public class EntitiesProcessor<T> {

    private final Supplier<Collection<T>> entities;
    private final Predicate<Boolean> isProcessed = it -> it;

    public EntitiesProcessor(Supplier<Collection<T>> entitiesSupplier) {
        this.entities = entitiesSupplier;
    }

    /**
     * Process the entities retrieved by the supplier with the function specified
     *
     * @param process the function that will be applied to the entities. Returns true if the entity is processed, false otherwise
     * @return the processed entities count
     */
    public long doProcess(Function<T, Boolean> process) {
        return entities.get().stream()
                .map(process)
                .filter(isProcessed)
                .count();
    }
}
