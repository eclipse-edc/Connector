/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file.zip.provision;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

public class ZipArchiveProvisioner implements Provisioner<ZipArchiveResourceDefinition, ZipArchiveResourceDefinitionProvisioned> {

    private final Monitor monitor;
    private ProvisionContext context;

    public ZipArchiveProvisioner(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void initialize(ProvisionContext context) {
        this.context = context;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof ZipArchiveResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof ZipArchiveResourceDefinitionProvisioned;
    }

    @Override
    public ResponseStatus provision(ZipArchiveResourceDefinition resourceDefinition) {

        var folderName = resourceDefinition.getDirectory();
        var fileName = resourceDefinition.getZipFileName();
        var dirPath = Paths.get(folderName);

        if (!fileName.endsWith(".zip")) {
            fileName += ".zip";
        }
        var zipPath = Paths.get(folderName, fileName);

        try {
            if (Files.notExists(dirPath)) {
                Files.createDirectory(dirPath);
            }

            if (Files.notExists(zipPath)) {
                var os = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
                os.write(new byte[0]);
                os.close();
            }
        } catch (IOException e) {
            monitor.severe("create zip archive failed", e);
            return ResponseStatus.FATAL_ERROR;
        }

        var provisionedResource = ZipArchiveResourceDefinitionProvisioned.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .directory(folderName)
                .zipFileName(fileName)
                .build();

        context.callback(provisionedResource, null);

        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(ZipArchiveResourceDefinitionProvisioned provisionedResource) {
        return ResponseStatus.OK;
    }
}
