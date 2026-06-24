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

package org.eclipse.edc.controlplane.tasks.store;

import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

/**
 * Interface for storing and retrieving {@link Task}s. Implementations must be thread-safe.
 */
public interface TaskStore {

    /**
     * Creates a new task in the store.
     *
     * @param task the task to create
     */
    StoreResult<Void> create(Task task);

    /**
     * Fetches tasks matching the given query spec, and locks them for update. The tasks will be automatically unlocked when the transaction completes.
     *
     * @param querySpec the query spec to filter tasks
     * @return a list of tasks matching the query spec
     */
    List<Task> fetchForUpdate(QuerySpec querySpec);

    /**
     * Deletes the task with the given id.
     *
     * @param id the id of the task to delete
     */
    StoreResult<Void> delete(String id);

    /**
     * Updates an existing task in the store. The task is identified by its id, and all fields will be updated to match the given task.
     *
     * @param task the task to update
     */
    StoreResult<Void> update(Task task);

    /**
     * Finds a task by its id.
     *
     * @param id the id of the task to find
     * @return the task with the given id, or null if no such task exists
     */
    Task findById(String id);
}
