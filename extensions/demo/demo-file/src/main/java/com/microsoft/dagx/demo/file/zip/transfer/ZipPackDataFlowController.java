/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file.zip.transfer;


import com.microsoft.dagx.demo.file.zip.schema.ZipSchema;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

public class ZipPackDataFlowController implements DataFlowController {

    private final Monitor monitor;

    public ZipPackDataFlowController(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return dataRequest.getDataDestination().getType().equals(ZipSchema.TYPE);
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {

        var zipDirectory = dataRequest.getDataDestination().getProperties().get(ZipSchema.DIRECTORY);
        var zipName = dataRequest.getDataDestination().getProperties().get(ZipSchema.NAME);
        var zipPath = Paths.get(zipDirectory, zipName);

        var entityId = dataRequest.getDataEntry().getId();
        var fileDirectory = dataRequest.getDataEntry().getCatalog().getPropertiesForEntity(entityId).getProperty("directory");
        var fileName = dataRequest.getDataEntry().getCatalog().getPropertiesForEntity(entityId).getProperty("name");
        var filePath = Paths.get(fileDirectory, fileName);

        try {

            var fileNameWithDuplicateCounter = getNameWithDuplicateCounter(fileName, zipPath.toFile(), 0);

            var uri = URI.create("jar:" + zipPath.toUri());
            var zipFileSystem = FileSystems.newFileSystem(uri, new HashMap<>());

            // append file to existing zip archive
            Files.copy(filePath, zipFileSystem.getPath(fileNameWithDuplicateCounter));
            zipFileSystem.close();

            monitor.info("write " + fileNameWithDuplicateCounter + " to " + zipPath);

        } catch (IOException e) {
            monitor.severe("could not write file", e);
            return new DataFlowInitiateResponse(ResponseStatus.FATAL_ERROR, e.getMessage());
        }

        return DataFlowInitiateResponse.OK;
    }

    private String getNameWithDuplicateCounter(String rawName, File zipFile, int findings) throws IOException {
        var name = findings == 0 ? rawName : rawName + " (" + findings + ")";

        var input = new ZipInputStream(new FileInputStream(zipFile));
        var nextEntry = input.getNextEntry();

        while (nextEntry != null) {
            if (nextEntry.getName().equals(name)) {
                return getNameWithDuplicateCounter(rawName, zipFile, findings + 1);
            }
            nextEntry = input.getNextEntry();
        }

        return name;
    }
}
