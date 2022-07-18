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
 *       Masatake Iwasaki (NTT DATA) - initial code
 *
 */

package org.eclipse.dataspaceconnector.core.event;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Holder class for a shared {@link ExecutorService} across event router implementations.
 */
public class EventExecutorServiceContainer {
    private final ExecutorService executorService;

    public EventExecutorServiceContainer(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
    }

    public @NotNull ExecutorService getExecutorService() {
        return executorService;
    }
}
