package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a certain subset of catalog instances inside a subset of a data space.
 * For example, a partition could be based on geographic regions. The most obvious use case for a {@code PartitionManager}
 * would be to dispatch {@link Crawler} objects to gather catalogs from other connectors.
 */
public interface PartitionManager {

    /**
     * Updates the current execution plan with the new one.
     * Implementors should employ their best effort to merge two ExecutionPlans. If merging is not possible (e.g. when using Durations)
     * the new Execution Plan overrides the old one
     *
     * @param newPlan A new (updated) execution plan
     * @return the merged execution plan
     */
    @NotNull ExecutionPlan update(ExecutionPlan newPlan);

    /**
     * Schedules all crawlers for execution according to this ExecutionPlan
     */
    void schedule(ExecutionPlan executionPlan);

    /**
     * Waits until all crawlers have finished their task.
     */
    void stop();
}
