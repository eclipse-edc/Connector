/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Add stop method
 *
 */

package org.eclipse.edc.crawler.spi.model;

/**
 * Interface for any sort of planned execution of a {@link Runnable} task.
 */
public interface ExecutionPlan {

    /**
     * Execute the task. While not strictly required, spawning another
     * thread it is highly recommended e.g. by forwarding to an {@link java.util.concurrent.Executor}
     *
     * @param task A runnable
     */
    void run(Runnable task);

    /**
     * Stops the execution of the task. It is recommended to ensure that all
     * resources are properly released and any ongoing tasks are gracefully
     * terminated.
     */
    void stop();
}
