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

import java.util.Objects;

/**
 * Base class for task payloads that are associated with a process. This can be used for tasks that need to track the state of a process, such as a transfer process or a contract negotiation.
 */
public abstract class ProcessTaskPayload implements TaskPayload {

    protected String processType;
    protected String processId;
    protected Integer processState;

    public String getProcessId() {
        return processId;
    }

    public Integer getProcessState() {
        return processState;
    }

    public String getProcessType() {
        return processType;
    }

    public abstract static class Builder<T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> {

        protected final T task;

        protected Builder(T task) {
            this.task = task;
        }

        public abstract B self();


        public B processId(String processId) {
            task.processId = processId;
            return self();
        }

        public B processType(String processType) {
            task.processType = processType;
            return self();
        }

        public B processState(Integer processState) {
            task.processState = processState;
            return self();
        }


        public T build() {
            Objects.requireNonNull(task.processId);
            Objects.requireNonNull(task.processState);
            Objects.requireNonNull(task.processType);
            return task;
        }
    }
}
