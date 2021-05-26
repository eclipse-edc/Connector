package com.microsoft.dagx.demo.file.transfer.provision.artifact;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RandomFileArtifactProvisioner implements Provisioner<RandomFileArtifactResourceDefinition, RandomFileArtifactResourceDefinitionProvisioned> {

    private final Monitor monitor;
    private ProvisionContext context;

    public RandomFileArtifactProvisioner(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void initialize(ProvisionContext context) {
        this.context = context;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof RandomFileArtifactResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof RandomFileArtifactResourceDefinitionProvisioned;
    }

    @Override
    public ResponseStatus provision(RandomFileArtifactResourceDefinition resourceDefinition) {

        var targetFileName = resourceDefinition.getTargetFileName();
        var sourceDirectory = "/tmp";
        var sourceFileName = targetFileName + ".tmp";
        var randomData = UUID.randomUUID().toString().getBytes();

        if (resourceDefinition.isCompressionRequested()) {
            try {
                randomData = Zip(targetFileName, randomData);
                targetFileName += ".zip";
            } catch (IOException e) {
                monitor.severe("zip data failed", e);
                return ResponseStatus.FATAL_ERROR;
            }
        }

        try {
            Files.write(Paths.get(sourceDirectory, sourceFileName), randomData);
        } catch (IOException e) {
            monitor.severe("write data failed", e);
            return ResponseStatus.FATAL_ERROR;
        }

        var provisionedResource = RandomFileArtifactResourceDefinitionProvisioned.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .destinationFolderName(resourceDefinition.getTargetFolderName())
                .destinationFileName(targetFileName)
                .sourceFileName(sourceFileName)
                .sourceFolderName(sourceDirectory)
                .build();

        context.callback(provisionedResource, null);

        return ResponseStatus.OK;
    }

    private byte[] Zip(String fileName, byte[] data) throws IOException {

        var byteOut = new ByteArrayOutputStream();
        var zipOut = new ZipOutputStream(byteOut);
        var entry = new ZipEntry(fileName);

        zipOut.putNextEntry(entry);
        zipOut.write(data, 0, data.length);

        zipOut.closeEntry();
        zipOut.close();

        return byteOut.toByteArray();
    }

    @Override
    public ResponseStatus deprovision(RandomFileArtifactResourceDefinitionProvisioned provisionedResource) {
        return ResponseStatus.OK;
    }
}
