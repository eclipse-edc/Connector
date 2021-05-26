package com.microsoft.dagx.demo.file.transfer.provision.artifact;

import com.microsoft.dagx.demo.file.schema.FileSchema;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

import java.util.Objects;

public class RandomFileArtifactResourceDefinitionProvisioned extends ProvisionedDataDestinationResource {

    private String destinationFolderName;
    private String destinationFileName;
    private String sourceFolderName;
    private String sourceFileName;

    private RandomFileArtifactResourceDefinitionProvisioned() {
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .type(FileSchema.TYPE)
                .property(FileSchema.TARGET_FILE_NAME, destinationFileName)
                .property(FileSchema.TARGET_DIRECTORY, destinationFolderName)
                .keyName("file")
                .build();
    }

    @Override
    public String getResourceName() {
        return sourceFileName;
    }

    public String getSourceDirectory() {
        return sourceFolderName;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public static class Builder extends ProvisionedResource.Builder<RandomFileArtifactResourceDefinitionProvisioned, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder sourceFolderName(String sourceFolderName) {
            provisionedResource.sourceFolderName = sourceFolderName;
            return this;
        }

        public Builder sourceFileName(String sourceFileName) {
            provisionedResource.sourceFileName = sourceFileName;
            return this;
        }

        public Builder destinationFolderName(String destinationFolderName) {
            provisionedResource.destinationFolderName = destinationFolderName;
            return this;
        }

        public Builder destinationFileName(String destinationFileName) {
            provisionedResource.destinationFileName = destinationFileName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(provisionedResource.sourceFileName, "sourceFileName");
            Objects.requireNonNull(provisionedResource.sourceFolderName, "sourceFolderName");
            Objects.requireNonNull(provisionedResource.destinationFolderName, "destinationFolderName");
            Objects.requireNonNull(provisionedResource.destinationFileName, "destinationFileName");
        }

        private Builder() {
            super(new RandomFileArtifactResourceDefinitionProvisioned());
        }
    }
}
