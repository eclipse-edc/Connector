package org.eclipse.dataspaceconnector.catalog.spi.model;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An ExecutionPlan that executes periodically according to a given schedule.
 */
public class RecurringExecutionPlan implements ExecutionPlan {
    private final Duration schedule;
    private final boolean withInitialDelay;

    /**
     * Instantiates the {@code RecurringExecutionPlan}.
     *
     * @param schedule         A time span used for initial delay and for the period.
     * @param withInitialDelay Specifies whether the execution plan should run right away or after an initial delay passes. That delay is equal to {@code schedule}.
     */
    public RecurringExecutionPlan(Duration schedule, boolean withInitialDelay) {
        this.schedule = schedule;
        this.withInitialDelay = withInitialDelay;
    }

    /**
     * Instantiates the {@code RecurringExecutionPlan}.
     *
     * @param schedule         A time span used for initial delay and for the period.
     */
    public RecurringExecutionPlan(Duration schedule) {
        this(schedule, false);
    }


    @Override
    public ExecutionPlan merge(ExecutionPlan other) {
        return other; //at this time we just have the schedule, so we can safely overwrite
    }

    @Override
    public void run(Runnable task) {
        var ses = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = withInitialDelay ? schedule.toMillis() : 0;
        ses.scheduleAtFixedRate(task, initialDelay, schedule.toMillis(), TimeUnit.MILLISECONDS);
    }
}
