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

import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

public class TaskServiceImpl implements TaskService {

    private final TaskStore taskStore;
    private final TaskObservable taskObservable;
    private final TransactionContext transactionContext;


    public TaskServiceImpl(TaskStore taskStore, TaskObservable taskObservable, TransactionContext transactionContext) {
        this.taskStore = taskStore;
        this.taskObservable = taskObservable;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<Task> create(Task task) {
        return transactionContext.execute(() -> {
            var storeResult = taskStore.create(task).onSuccess(v -> {
                taskObservable.invokeForEach(l -> l.created(task));
            });
            return ServiceResult.from(storeResult).map(v -> task);
        });
    }

    @Override
    public List<Task> fetchLatestTask(QuerySpec query) {
        return transactionContext.execute(() -> taskStore.fetchForUpdate(query));
    }

    @Override
    public ServiceResult<Void> delete(String id) {
        return transactionContext.execute(() -> ServiceResult.from(taskStore.delete(id)));
    }

    @Override
    public ServiceResult<Void> update(Task task) {
        return transactionContext.execute(() -> ServiceResult.from(taskStore.update(task)));
    }

    @Override
    public Task findById(String id) {
        return transactionContext.execute(() -> taskStore.findById(id));
    }
}
