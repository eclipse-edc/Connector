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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.spi.persistence;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Define a store that can be used within a state machine
 */
public interface StateEntityStore<T> {

    /**
     * Returns a {@link Criterion} to filter state
     *
     * @param stateCode the entity state code.
     * @return a criterion.
     */
    static Criterion hasState(int stateCode) {
        return new Criterion("state", "=", stateCode);
    }

    static Criterion isNotPending() {
        return new Criterion("pending", "=", false);
    }

    /**
     * Finds the entity for the id or null.
     *
     * @param id the id.
     * @return the entity if found, null otherwise.
     */
    @Nullable
    T findById(String id);

    /**
     * Returns a list of not leased entities that satisfy the filter criteria.
     * <p>
     * Implementors MUST handle these requirements: <p>
     * <ul>
     *     <li>
     *         * entities should be fetched from the oldest to the newest, by a timestamp that reports the last state transition on the entity
     *         <p><p>
     *     </li>
     *     <li>
     *         * fetched entities should be leased for a configurable timeout, that will be released after the timeout expires or when the entity will be updated.
     *         This will avoid consecutive fetches in the state machine loop
     *         <p><p>
     *     </li>
     * </ul>
     *
     * @param max      The maximum amount of result items.
     * @param criteria The selection criteria.
     * @return A list of entities (at most _max_) that satisfy the selection criteria.
     */
    @NotNull
    List<T> nextNotLeased(int max, Criterion... criteria);

    /**
     * Find the entity by the passed id and lease it.
     * If the entity is already leased, will return a failure.
     *
     * @param id the entity id.
     * @return success if the entity is unleased, failure otherwise.
     */
    StoreResult<T> findByIdAndLease(String id);

    /**
     * Persists the entity. This follows UPSERT semantics, so if the object didn't exit before, it's created.
     *
     * @param entity the entity.
     */
    StoreResult<Void> save(T entity);
}
