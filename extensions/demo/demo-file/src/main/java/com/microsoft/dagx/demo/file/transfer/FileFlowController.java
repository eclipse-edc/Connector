package com.microsoft.dagx.demo.file.transfer;

import com.microsoft.dagx.demo.file.transfer.provision.artifact.RandomFileArtifactResourceDefinitionProvisioned;
import com.microsoft.dagx.demo.file.schema.FileSchema;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileFlowController implements DataFlowController {

    private final TransferProcessStore transferProcessStore;
    private final Monitor monitor;

    public FileFlowController(TransferProcessStore transferProcessStore, Monitor monitor) {
        this.transferProcessStore = transferProcessStore;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return dataRequest.getDataDestination().getType().equals(FileSchema.TYPE);
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {

        var transferProcess = transferProcessStore.find(dataRequest.getProcessId());
        var provisionedResourcesSets = transferProcess.getProvisionedResourceSet();
        if (provisionedResourcesSets == null) {
            return DataFlowInitiateResponse.OK;
        }

        var provisionedResources = provisionedResourcesSets.getResources();

        for (var provisionedResource : provisionedResources) {
            if (!(provisionedResource instanceof RandomFileArtifactResourceDefinitionProvisioned)) continue;

            var targetDirectory = dataRequest.getDataDestination().getProperties().get(FileSchema.TARGET_DIRECTORY);
            var targetFileName = dataRequest.getDataDestination().getProperties().get(FileSchema.TARGET_FILE_NAME);
            var targetPath = Paths.get(targetDirectory, targetFileName);


            var sourceDirectory = ((RandomFileArtifactResourceDefinitionProvisioned) provisionedResource).getSourceDirectory();
            var sourceFileName = ((RandomFileArtifactResourceDefinitionProvisioned) provisionedResource).getSourceFileName();
            var sourcePath = Paths.get(sourceDirectory, sourceFileName);

            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                monitor.info("write " + targetFileName + " to " + targetDirectory);
            } catch (IOException e) {
                monitor.severe("could not write file", e);
                return new DataFlowInitiateResponse(ResponseStatus.FATAL_ERROR, e.getMessage());
            }
        }

        return DataFlowInitiateResponse.OK;
    }
}
