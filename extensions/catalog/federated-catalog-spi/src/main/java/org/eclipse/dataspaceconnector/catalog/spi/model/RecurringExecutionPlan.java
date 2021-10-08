package org.eclipse.dataspaceconnector.catalog.spi.model;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An ExecutionPlan that executes periodically.
 */
public class RecurringExecutionPlan implements ExecutionPlan {
    private final Duration schedule;

    /**
     * Instantiates the {@code RecurringExecutionPlan}.
     *
     * @param schedule A time span used for initial delay and for the period.
     */
    public RecurringExecutionPlan(Duration schedule) {
        this.schedule = schedule;
    }


    @Override
    public ExecutionPlan merge(ExecutionPlan other) {
        return other; //at this time we just have the schedule, so we can safely overwrite
    }

    @Override
    public void run(Runnable task) {
        var ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(task, schedule.toMillis(), schedule.toMillis(), TimeUnit.MILLISECONDS);
    }
}
