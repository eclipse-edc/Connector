package com.microsoft.dagx.demo.file.transfer.provision.folder;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class FolderProvisioner implements Provisioner<FolderResourceDefinition, FolderResourceDefinitionProvisioned> {

    private final Monitor monitor;
    private ProvisionContext context;

    public FolderProvisioner(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void initialize(ProvisionContext context) {
        this.context = context;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof FolderResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof FolderResourceDefinitionProvisioned;
    }

    @Override
    public ResponseStatus provision(FolderResourceDefinition resourceDefinition) {

        var folderName = resourceDefinition.getTargetFolderName();
        var fileName = resourceDefinition.getTargetFileName();
        var folderPath = Paths.get(folderName);

        try {
            if (Files.notExists(folderPath)) {
                Files.createDirectory(folderPath);
            }
        } catch (IOException e) {
            monitor.severe("create directory failed", e);
            return ResponseStatus.FATAL_ERROR;
        }

        var provisionedResource = FolderResourceDefinitionProvisioned.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .isCompressRequested(resourceDefinition.getIsCompressionRequested())
                .targetFolderName(folderName)
                .targetFileName(fileName)
                .build();

        context.callback(provisionedResource, null);

        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(FolderResourceDefinitionProvisioned provisionedResource) {
        return ResponseStatus.OK;
    }
}
