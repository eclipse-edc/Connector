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

package org.eclipse.dataspaceconnector.samples.sample042;

import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

public class WatchdogExtension implements ServiceExtension {

    @Inject
    private TransferProcessManager manager;

    @Inject
    private TransferProcessStore store;

    @Inject
    private CommandHandlerRegistry commandHandlerRegistry;
    private Watchdog wd;

    @Override
    public void initialize(ServiceExtensionContext context) {
        commandHandlerRegistry.register(new CheckTimeoutCommandHandler(store, context.getClock(), context.getMonitor()));
        wd = new Watchdog(manager, context.getMonitor());
    }

    @Override
    public void start() {
        wd.start();
    }

    @Override
    public void shutdown() {
        wd.stop();
    }
}
