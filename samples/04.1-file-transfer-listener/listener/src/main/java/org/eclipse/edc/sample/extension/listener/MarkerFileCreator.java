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

import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

public class MarkerFileCreator implements TransferProcessListener {

    private final Monitor monitor;

    public MarkerFileCreator(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Callback invoked by the EDC framework when a transfer is about to be completed.
     *
     * @param process the transfer process that is about to be completed.
     */
    @Override
    public void preCompleted(final TransferProcess process) {
        Path path = Path.of(process.getDataRequest().getDataDestination().getProperty("path"));
        if (!Files.isDirectory(path)) {
            path = path.getParent();
        }
        path = path.resolve("marker.txt");

        try {
            Files.writeString(path, "Transfer complete");
            monitor.info(format("Transfer Listener successfully wrote file %s", path));
        } catch (IOException e) {
            monitor.warning(format("Could not write file %s", path), e);
        }
    }
}
