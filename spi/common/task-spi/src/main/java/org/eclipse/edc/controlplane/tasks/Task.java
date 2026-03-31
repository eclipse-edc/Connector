/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.tasks;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a task that can be scheduled for execution. A task has a payload, which contains the data needed to execute the task, and metadata such as the time at which the task should be executed and the number of times it has been retried.
 */
public class Task {
    protected String id;

    protected long at;
    protected int retryCount;

    protected String name;
    protected String group;

    protected TaskPayload payload;

    public long getAt() {
        return at;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    public TaskPayload getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Task.Builder toBuilder() {
        return Builder.newInstance()
                .id(id)
                .at(at)
                .payload(payload);
    }

    public static class Builder {

        protected final Task task;

        protected Builder() {
            task = new Task();
        }

        public static Task.Builder newInstance() {
            return new Task.Builder();
        }

        public Task.Builder id(String id) {
            task.id = id;
            return this;
        }

        public Task.Builder at(long at) {
            task.at = at;
            return this;
        }

        public Task.Builder payload(TaskPayload payload) {
            task.payload = payload;
            return this;
        }

        public Task.Builder retryCount(int retryCount) {
            task.retryCount = retryCount;
            return this;
        }

        public Task.Builder name(String name) {
            task.name = name;
            return this;
        }

        public Task.Builder group(String group) {
            task.group = group;
            return this;
        }

        public Task build() {
            if (task.id == null) {
                task.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(task.payload, "Task payload must be set");

            if (task.name == null) {
                task.name = task.payload.name();
            }
            if (task.group == null) {
                task.group = task.payload.group();
            }
            if (task.at == 0) {
                throw new IllegalStateException("Event 'at' field must be set");
            }
            Objects.requireNonNull(task.name, "Task name must be set");
            Objects.requireNonNull(task.group, "Task group must be set");

            return task;
        }

    }
}
