/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;

/**
 * Defines basic configuration for a {@link StateMachineManager}.
 */
@Settings
public record StateMachineConfiguration(

        @Setting(
                description = "The iteration wait time in milliseconds in the state machine.",
                key = "state-machine.iteration-wait-millis",
                defaultValue = DEFAULT_ITERATION_WAIT + ""
        )
        long iterationWaitMillis,

        @Setting(
                description = "The number of entities to be processed on every iteration.",
                key = "state-machine.batch-size",
                defaultValue = DEFAULT_BATCH_SIZE + ""
        )
        int batchSize,

        @Setting(
                description = "How many times a specific operation must be tried before failing with error",
                key = "send.retry.limit",
                defaultValue = DEFAULT_SEND_RETRY_LIMIT + ""
        )
        int sendRetryLimit,

        @Setting(
                description = "The base delay for the consumer negotiation retry mechanism in millisecond",
                key = "send.retry.base-delay.ms",
                defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + ""
        )
        int sendRetryBaseDelayMs

) {

    public static final long DEFAULT_ITERATION_WAIT = 1000;
    public static final int DEFAULT_BATCH_SIZE = 20;
    public static final int DEFAULT_SEND_RETRY_LIMIT = 7;
    public static final long DEFAULT_SEND_RETRY_BASE_DELAY = 1000L;

    public ExponentialWaitStrategy baseDelayExponentialWaitStrategy() {
        return new ExponentialWaitStrategy(sendRetryBaseDelayMs);
    }

    public EntityRetryProcessConfiguration entityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(sendRetryLimit, this::baseDelayExponentialWaitStrategy);
    }

    public ExponentialWaitStrategy iterationWaitExponentialWaitStrategy() {
        return new ExponentialWaitStrategy(iterationWaitMillis);
    }
}
