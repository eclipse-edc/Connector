package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;

import java.util.concurrent.CompletableFuture;

public interface PartitionManager {

    /**
     * Updates the current execution plan with the new one.
     * A best effort is attempted to merge two ExecutionPlans. If merging is not possible (e.g. when using Durations)
     * the new Execution Plan overrides the old one
     *
     * @param newPlan A new (updated) execution plan
     */
    void update(ExecutionPlan newPlan);

    /**
     * Schedules all crawlers for execution according to this ExecutionPlan
     */
    void schedule(ExecutionPlan executionPlan);

    /**
     * Waits until all crawlers have finished their task.
     *
     * @return A CompletableFuture that resolves once all Crawlers have finished
     */
    CompletableFuture<Void> waitForCompletion();
}
