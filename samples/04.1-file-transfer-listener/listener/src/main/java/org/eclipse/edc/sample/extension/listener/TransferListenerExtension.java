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
 *
 */

package org.eclipse.edc.sample.extension.listener;

import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class TransferListenerExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var transferProcessObservable = context.getService(TransferProcessObservable.class);
        var monitor = context.getMonitor();

        transferProcessObservable.registerListener(new MarkerFileCreator(monitor));
    }
}
