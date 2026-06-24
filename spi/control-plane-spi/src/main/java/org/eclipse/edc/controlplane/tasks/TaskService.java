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

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service for managing tasks. This service provides methods to create, fetch, delete, and update tasks.
 */
public interface TaskService {

    /**
     * Creates a new task.
     *
     * @param task the task to create
     */
    ServiceResult<Task> create(Task task);

    /**
     * Fetches tasks based on the provided query specification. The query specification can include filters, sorting, and pagination.
     *
     * @param querySpec the query specification to use for fetching tasks
     * @return a list of tasks that match the query specification
     */
    List<Task> fetchLatestTask(QuerySpec querySpec);

    /**
     * Deletes a task by its ID.
     *
     * @param id the ID of the task to delete
     * @return a ServiceResult indicating the success or failure of the delete operation
     */
    ServiceResult<Void> delete(String id);

    /**
     * Updates an existing task. The task must have a valid ID that corresponds to an existing task.
     *
     * @param task the task to update
     * @return a ServiceResult indicating the success or failure of the update operation
     */
    ServiceResult<Void> update(Task task);

    /**
     * Finds a task by its ID.
     *
     * @param id the ID of the task to find
     * @return the task with the specified ID, or null if no such task exists
     */
    Task findById(String id);
}
