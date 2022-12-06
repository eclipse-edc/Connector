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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.sample.extension.watchdog;

import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Watchdog {

    private final TransferProcessManager manager;
    private final Monitor monitor;
    private ScheduledExecutorService executor;

    public Watchdog(TransferProcessManager manager, Monitor monitor) {

        this.manager = manager;
        this.monitor = monitor;
    }

    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor();
        // run every 10 minutes, no initial delay
        executor.scheduleAtFixedRate(this::check, 10, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void check() {
        monitor.info("Running watchdog - submit command");
        manager.enqueueCommand(new CheckTransferProcessTimeoutCommand(3, TransferProcessStates.IN_PROGRESS, Duration.ofSeconds(10)));
    }
}
