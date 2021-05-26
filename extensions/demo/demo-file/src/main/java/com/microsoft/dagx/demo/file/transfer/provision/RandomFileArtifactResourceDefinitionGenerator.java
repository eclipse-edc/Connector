package com.microsoft.dagx.demo.file.transfer.provision;

import com.microsoft.dagx.demo.file.transfer.provision.artifact.RandomFileArtifactResourceDefinition;
import com.microsoft.dagx.demo.file.schema.FileSchema;
import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RandomFileArtifactResourceDefinitionGenerator implements ResourceDefinitionGenerator {
    @Override
    public @Nullable ResourceDefinition generate(TransferProcess process) {

        var destination = process.getDataRequest().getDataDestination();
        if (!destination.getType().equals(FileSchema.TYPE)) {
            return null;
        }

        var targetFileName = destination.getProperty(FileSchema.TARGET_FILE_NAME);
        var targetFolderName = destination.getProperty(FileSchema.TARGET_DIRECTORY);

        var isCompressionRequestedProperty = destination.getProperty(FileSchema.IS_COMPRESSION_REQUESTED);
        var isCompressionRequested = isCompressionRequestedProperty != null && isCompressionRequestedProperty.equalsIgnoreCase("true");

        return RandomFileArtifactResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .targetFileName(targetFileName)
                .targetFolderName(targetFolderName)
                .isCompressionRequested(isCompressionRequested)
                .build();
    }
}