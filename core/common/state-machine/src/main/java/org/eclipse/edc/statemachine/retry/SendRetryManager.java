/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service enabling a "long retry" mechanism when sending entities across applications.
 * Implementations may support a retry strategy (e.g. an exponential wait mechanism
 * so as not to overflow the remote service when it becomes available again).
 *
 */
public interface SendRetryManager {
    /**
     * Determines whether the given entity may be sent at this time, or the system
     * should wait and send the entity later.
     *
     * @param entity entity to be evaluated.
     * @return {@code true} if the entity should not be sent at this time.
     * @param <T> stateful entity type
     */
    <T extends StatefulEntity<T>> boolean shouldDelay(T entity);

    /**
     * Determines whether retries for sending the given entity have been exhausted.
     *
     * @param entity entity to be evaluated.
     * @return {@code true} if the entity should not be sent anymore.
     * @param <T> stateful entity type
     */
    <T extends StatefulEntity<T>> boolean retriesExhausted(T entity);

    /**
     * Initialize a simple process that needs to be retried if it does not succeed
     */
    <T extends StatefulEntity<T>> SimpleRetryProcess<T> doSimpleProcess(T entity, Supplier<Boolean> process);

    /**
     * Initialize a synchronous process that needs to be retried if it does not succeed
     */
    <T extends StatefulEntity<T>, C> StatusResultRetryProcess<T, C> doSyncProcess(T entity, String description, Supplier<StatusResult<C>> process);

    /**
     * Initialize an asynchronous process that needs to be retried if it does not succeed
     */
    <T extends StatefulEntity<T>, C> CompletableFutureRetryProcess<T, C> doAsyncProcess(T entity, String description, Supplier<CompletableFuture<C>> process);
}
