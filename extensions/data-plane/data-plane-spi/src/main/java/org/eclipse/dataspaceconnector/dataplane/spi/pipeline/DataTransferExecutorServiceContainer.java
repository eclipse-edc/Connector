/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Holder class for a shared {@link ExecutorService} across data transfer implementations.
 */
public class DataTransferExecutorServiceContainer {
    private final ExecutorService executorService;

    public DataTransferExecutorServiceContainer(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
    }

    public @NotNull ExecutorService getExecutorService() {
        return executorService;
    }
}
