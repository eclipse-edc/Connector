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

package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataStreamPublisher;
import org.eclipse.dataspaceconnector.spi.transfer.inline.StreamContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileTransferDataStreamPublisher implements DataStreamPublisher {
    private final Monitor monitor;
    private final DataAddressResolver dataAddressResolver;

    public FileTransferDataStreamPublisher(Monitor monitor, DataAddressResolver dataAddressResolver) {
        this.monitor = monitor;
        this.dataAddressResolver = dataAddressResolver;
    }

    @Override
    public void initialize(StreamContext context) {
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return "file".equalsIgnoreCase(dataRequest.getDataDestination().getType());
    }

    @Override
    public Result<Void> notifyPublisher(DataRequest dataRequest) {
        var source = dataAddressResolver.resolveForAsset(dataRequest.getAssetId());
        var destination = dataRequest.getDataDestination();

        // verify source path
        String sourceFileName = source.getProperty("filename");
        var sourcePath = Path.of(source.getProperty("path"), sourceFileName);
        if (!sourcePath.toFile().exists()) {
            return Result.failure("Source file " + sourcePath + " does not exist!");
        }

        // verify destination path
        var destinationPath = Path.of(destination.getProperty("path"));
        if (!destinationPath.toFile().exists()) { //interpret as directory
            monitor.debug("Destination path " + destinationPath + " does not exist, will attempt to create");
            try {
                Files.createDirectory(destinationPath);
                monitor.debug("Successfully created destination path " + destinationPath);
            } catch (IOException e) {
                String message = "Error creating directory: " + e.getMessage();
                monitor.severe(message);
                return Result.failure(message);
            }
        } else if (destinationPath.toFile().isDirectory()) {
            destinationPath = Path.of(destinationPath.toString(), sourceFileName);
        }

        try {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            monitor.info("Successfully copied file to " + destinationPath);
        } catch (IOException e) {
            String message = "Error copying file " + e.getMessage();
            monitor.severe(message);
            return Result.failure(message);
        }
        return Result.success();
    }
}
