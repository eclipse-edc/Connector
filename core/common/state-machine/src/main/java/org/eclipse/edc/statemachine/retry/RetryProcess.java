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

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.function.Consumer;

/**
 * Represent a process on a {@link StatefulEntity} that is retried after a certain delay if it fails.
 * This works only used on a state machine, where states are persisted.
 * The process is a unit of logic that can be executed on the entity.
 */
public abstract class RetryProcess<E extends StatefulEntity<E>, T extends RetryProcess<E, T>> {

    private final E entity;
    private final SendRetryManager sendRetryManager;
    protected Consumer<E> onDelay;

    protected RetryProcess(E entity, SendRetryManager sendRetryManager) {
        this.entity = entity;
        this.sendRetryManager = sendRetryManager;
    }

    /**
     *  Execute some logic on the {@link E} entity, return true if the process happened, false otherwise.
     */
    abstract boolean process(E entity, String description);

    /**
     * If entity is not yet ready to be processed executes {@link #onDelay} handler and return false,
     * otherwise processes it.
     *
     * @param description the process description.
     * @return true if the process happened, false otherwise.
     */
    public boolean execute(String description) {
        if (sendRetryManager.shouldDelay(entity)) {
            if (onDelay != null) {
                onDelay.accept(entity);
            }
            return false;
        }

        return process(entity, description);
    }

    /**
     * Handler that is called if the entity is not yet ready for processing
     */
    public T onDelay(Consumer<E> onDelay) {
        this.onDelay = onDelay;
        return (T) this;
    }

    /**
     * Checks if retries are exhausted
     */
    protected boolean retriesExhausted(E entity) {
        return sendRetryManager.retriesExhausted(entity);
    }
}
