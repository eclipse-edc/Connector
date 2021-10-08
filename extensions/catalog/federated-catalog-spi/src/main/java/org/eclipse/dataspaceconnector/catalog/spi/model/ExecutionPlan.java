package org.eclipse.dataspaceconnector.catalog.spi.model;

import java.time.Duration;

public class ExecutionPlan {
    private final Duration schedule;

    public ExecutionPlan(Duration schedule) {
        this.schedule = schedule;
    }


    public Duration getSchedule() {
        return schedule;
    }

    public ExecutionPlan merge(ExecutionPlan other) {
        return other; //at this time we just have the schedule, so we can safely overwrite
    }
}
