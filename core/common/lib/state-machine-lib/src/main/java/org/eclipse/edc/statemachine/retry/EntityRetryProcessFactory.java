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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.time.Clock;

/**
 * Permit to instantiate a {@link RetryProcessor}. Please look at {@link RetryProcessor} for further details.
 */
public class EntityRetryProcessFactory {
    private final Monitor monitor;
    private final EntityRetryProcessConfiguration configuration;
    private final Clock clock;

    public EntityRetryProcessFactory(Monitor monitor, Clock clock, EntityRetryProcessConfiguration configuration) {
        this.monitor = monitor;
        this.clock = clock;
        this.configuration = configuration;
    }

    /**
     * Initialize a {@link RetryProcessor} on the passed entity.
     *
     * @param entity the entity.
     * @return a retry processor.
     */
    public <E extends StatefulEntity<E>, C> RetryProcessor<E, C> retryProcessor(E entity) {
        return new RetryProcessor<>(entity, monitor, clock, configuration);
    }

}
