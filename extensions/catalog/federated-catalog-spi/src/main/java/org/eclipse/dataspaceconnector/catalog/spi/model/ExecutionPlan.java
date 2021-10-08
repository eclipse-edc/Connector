package org.eclipse.dataspaceconnector.catalog.spi.model;

/**
 * Interface for any sort of planned execution of a {@link Runnable} task.
 */
public interface ExecutionPlan {
    /**
     * Updates this execution plan and the {@code other} execution plan. What this means is highly dependent
     * on the implementation, e.g. two cron jobs with different schedules could adopt both schedules, one, or the other.
     *
     * @param other the other execution plan
     * @return the merged execution plan.
     */
    ExecutionPlan merge(ExecutionPlan other);

    /**
     * Execute the task. While not strictly required, spawning another
     * thread it is highly recommended e.g. by forwarding to an {@link java.util.concurrent.Executor}
     *
     * @param task A runnable
     */
    void run(Runnable task);
}
