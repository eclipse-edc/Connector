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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *       Microsoft Corporation - added fields
 *
 */

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.retry.WaitStrategy;

import java.util.function.Supplier;

public class EntityRetryProcessConfiguration {

    private final int retryLimit;
    private final Supplier<WaitStrategy> delayStrategySupplier;

    public EntityRetryProcessConfiguration(int retryLimit, Supplier<WaitStrategy> delayStrategySupplier) {
        this.retryLimit = retryLimit;
        this.delayStrategySupplier = delayStrategySupplier;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public Supplier<WaitStrategy> getDelayStrategySupplier() {
        return delayStrategySupplier;
    }
}
