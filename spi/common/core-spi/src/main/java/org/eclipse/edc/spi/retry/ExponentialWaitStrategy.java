/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - adapt class for negotiation process
 *
 */

package org.eclipse.edc.spi.retry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements an exponential backoff strategy for successive retries.
 */
public class ExponentialWaitStrategy implements WaitStrategy {

    private final long successWaitPeriodMillis;
    private final AtomicInteger errorCount = new AtomicInteger(0);

    public ExponentialWaitStrategy(long successWaitPeriodMillis) {
        this.successWaitPeriodMillis = successWaitPeriodMillis;
    }

    @Override
    public long waitForMillis() {
        return successWaitPeriodMillis;
    }

    @Override
    public void success() {
        errorCount.set(0);
    }

    @Override
    public void failures(int numberOfFailures) {
        errorCount.addAndGet(numberOfFailures);
    }

    @Override
    public long retryInMillis() {
        var retryCount = errorCount.getAndIncrement();
        var exponentialMultiplier = 1L << retryCount; // = Math.pow(2, retryCount)
        return exponentialMultiplier * successWaitPeriodMillis;
    }
}
