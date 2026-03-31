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

package org.eclipse.edc.controlplane.transfer.process.tasks.listener;

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskPayload;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.transfer.spi.tasks.CompleteDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.PrepareTransfer;
import org.eclipse.edc.controlplane.transfer.spi.tasks.ResumeDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferStart;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SignalDataflowStarted;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SuspendDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TerminateDataFlow;
import org.eclipse.edc.spi.EdcException;

import java.time.Clock;

public class TransferProcessStateListener implements TransferProcessListener {

    private final TaskService taskService;
    private final Clock clock;

    public TransferProcessStateListener(TaskService taskService, Clock clock) {
        this.taskService = taskService;
        this.clock = clock;
    }

    @Override
    public void initiated(TransferProcess process) {
        var task = baseBuilder(PrepareTransfer.Builder.newInstance(), process)
                .build();
        storeTask(task);
    }

    @Override
    public void startupRequested(TransferProcess process) {
        var task = baseBuilder(SignalDataflowStarted.Builder.newInstance(), process)
                .build();

        storeTask(task);
    }

    @Override
    public void suspendingRequested(TransferProcess process) {
        var task = baseBuilder(SuspendDataFlow.Builder.newInstance(), process)
                .build();

        storeTask(task);
    }

    @Override
    public void suspending(TransferProcess process) {
        suspendingRequested(process);
    }

    @Override
    public void startingRequested(TransferProcess process) {
        var task = baseBuilder(SendTransferStart.Builder.newInstance(), process)
                .build();

        storeTask(task);
    }

    @Override
    public void terminatingRequested(TransferProcess process) {
        var task = baseBuilder(TerminateDataFlow.Builder.newInstance(), process)
                .build();

        storeTask(task);
    }

    @Override
    public void terminating(TransferProcess process) {
        terminatingRequested(process);
    }

    @Override
    public void completingRequested(TransferProcess process) {
        var task = baseBuilder(CompleteDataFlow.Builder.newInstance(), process)
                .build();

        storeTask(task);
    }

    @Override
    public void completing(TransferProcess process) {
        completingRequested(process);
    }

    @Override
    public void resuming(TransferProcess process) {
        var task = baseBuilder(ResumeDataFlow.Builder.newInstance(), process)
                .build();

        storeTask(task);
    }

    protected void storeTask(TaskPayload payload) {
        var task = Task.Builder.newInstance()
                .at(clock.millis())
                .payload(payload)
                .build();
        taskService.create(task)
                .orElseThrow(f -> new EdcException("Failed to create task: " + f.getFailureDetail()));
    }

    protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, TransferProcess transferProcess) {
        return builder.processId(transferProcess.getId())
                .processState(transferProcess.getState())
                .processType(transferProcess.getType().name());
    }
}
