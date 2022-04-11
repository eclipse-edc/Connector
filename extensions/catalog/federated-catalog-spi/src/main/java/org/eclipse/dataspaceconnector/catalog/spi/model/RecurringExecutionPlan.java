/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi.model;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An ExecutionPlan that executes periodically according to a given schedule.
 */
public class RecurringExecutionPlan implements ExecutionPlan {
    private final Duration schedule;
    private final Duration withInitialDelay;

    /**
     * Instantiates the {@code RecurringExecutionPlan}.
     *
     * @param schedule     A time span used for initial delay and for the period.
     * @param initialDelay Specifies whether the execution plan should run right away or after an initial delay passes. .
     */
    public RecurringExecutionPlan(Duration schedule, Duration initialDelay) {
        this.schedule = schedule;
        withInitialDelay = initialDelay;
    }

    /**
     * Instantiates the {@code RecurringExecutionPlan} with no initial delay
     *
     * @param schedule A time span used for initial delay and for the period.
     */
    public RecurringExecutionPlan(Duration schedule) {
        this(schedule, Duration.ofSeconds(0));
    }


    @Override
    public ExecutionPlan merge(ExecutionPlan other) {
        return other; //at this time we just have the schedule, so we can safely overwrite
    }

    @Override
    public void run(Runnable task) {
        var ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(task, withInitialDelay.toMillis(), schedule.toMillis(), TimeUnit.MILLISECONDS);
    }
}
