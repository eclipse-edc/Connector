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

package org.eclipse.edc.controlplane.tasks.defaults;

import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InMemoryTaskStore implements TaskStore {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final QueryResolver<Task> queryResolver;

    public InMemoryTaskStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(Task.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Void> create(Task task) {
        tasks.put(task.getId(), task);
        return StoreResult.success();
    }

    @Override
    public List<Task> fetchForUpdate(QuerySpec querySpec) {
        return queryResolver.query(tasks.values().stream(), querySpec)
                .collect(Collectors.toList());
    }

    @Override
    public StoreResult<Void> delete(String id) {
        var prev = tasks.remove(id);
        if (prev != null) return StoreResult.success();
        return StoreResult.notFound(format("Task with id %s not found", id));
    }

    @Override
    public StoreResult<Void> update(Task task) {
        var prev = tasks.computeIfPresent(task.getId(), (k, v) -> task);
        if (prev != null) return StoreResult.success();
        return StoreResult.notFound(format("Task with id %s not found", task.getId()));
    }

    @Override
    public Task findById(String id) {
        return tasks.get(id);
    }
}
