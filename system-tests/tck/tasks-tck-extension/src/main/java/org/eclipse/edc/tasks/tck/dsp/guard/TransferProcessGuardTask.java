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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.transfer.spi.tasks.CompleteDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.ResumeDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SuspendDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TerminateDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TransferProcessGuardTask {

    private final TaskService taskService;
    private final Clock clock;

    public TransferProcessGuardTask(TaskService taskService, Clock clock) {
        this.taskService = taskService;
        this.clock = clock;
    }

    public Consumer<TransferProcess> suspendAndResumeDataFlow() {
        var count = new AtomicInteger(0);
        return (process) -> {
            if (count.get() == 0) {
                count.incrementAndGet();
                suspendDataFlow(process);
            } else if (count.get() == 1) {
                count.set(0);
                completeDataFlow(process);
            }
        };
    }

    public void terminateDataFlow(TransferProcess transferProcess) {
        transferProcess.transitionTerminating();
        storeTask(baseBuilder(TerminateDataFlow.Builder.newInstance(), transferProcess).build());
    }

    public void resumingRequestedDataFlow(TransferProcess transferProcess) {
        transferProcess.transitionResumingRequested();
        storeTask(baseBuilder(ResumeDataFlow.Builder.newInstance(), transferProcess).build());
    }

    public void resumeDataFlow(TransferProcess transferProcess) {
        transferProcess.transitionResuming();
        storeTask(baseBuilder(ResumeDataFlow.Builder.newInstance(), transferProcess).build());
    }

    public void completeDataFlow(TransferProcess transferProcess) {
        transferProcess.transitionCompleting();
        storeTask(baseBuilder(CompleteDataFlow.Builder.newInstance(), transferProcess).build());
    }

    public void suspendDataFlow(TransferProcess transferProcess) {
        transferProcess.transitionSuspending("suspending");
        storeTask(baseBuilder(SuspendDataFlow.Builder.newInstance(), transferProcess).build());
    }

    public void suspendAndResumeDataFlow(TransferProcess transferProcess) {
        transferProcess.transitionSuspending("suspending");
        storeTask(baseBuilder(SuspendDataFlow.Builder.newInstance(), transferProcess).build());
    }

    private void storeTask(TransferProcessTaskPayload payload) {
        var task = Task.Builder.newInstance().at(clock.millis())
                .payload(payload)
                .build();
        taskService.create(task);
    }

    protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, TransferProcess transferProcess) {
        return builder.processId(transferProcess.getId())
                .processState(transferProcess.getState())
                .processType(transferProcess.getType().name());
    }
}
