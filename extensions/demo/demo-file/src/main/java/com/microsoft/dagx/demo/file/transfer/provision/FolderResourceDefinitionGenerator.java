package com.microsoft.dagx.demo.file.transfer.provision;

import com.microsoft.dagx.demo.file.transfer.provision.folder.FolderResourceDefinition;
import com.microsoft.dagx.demo.file.schema.FileSchema;
import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FolderResourceDefinitionGenerator implements ResourceDefinitionGenerator {
    @Override
    public @Nullable ResourceDefinition generate(TransferProcess process) {

        if (process.getDataRequest().getDestinationType() == null ||
                !process.getDataRequest().getDestinationType().equals("dagx:file")) {
            return null;
        }

        var folderName = process.getDataRequest().getDataDestination().getProperty(FileSchema.TARGET_DIRECTORY);
        var fileName = process.getDataRequest().getDataDestination().getProperty(FileSchema.TARGET_FILE_NAME);

        var isCompressionRequestedProperty = process.getDataRequest().getDataDestination().getProperty(FileSchema.IS_COMPRESSION_REQUESTED);
        var isCompressionRequested = isCompressionRequestedProperty != null && isCompressionRequestedProperty.equalsIgnoreCase("true");

        return FolderResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .targetFileName(fileName)
                .isCompressionRequested(isCompressionRequested)
                .targetFolderName(folderName).build();
    }
}
