package com.microsoft.dagx.demo.file.transfer.provision.folder;

import com.microsoft.dagx.demo.file.schema.FileSchema;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

import java.util.Objects;


public class FolderResourceDefinitionProvisioned extends ProvisionedDataDestinationResource {

    private String targetFolderName;
    private String targetFileName;
    private boolean isCompressRequested;

    private FolderResourceDefinitionProvisioned() {
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .type(FileSchema.TYPE)
                .property(FileSchema.TARGET_FILE_NAME, targetFileName)
                .property(FileSchema.TARGET_DIRECTORY, targetFolderName)
                .property(FileSchema.IS_COMPRESSION_REQUESTED, isCompressRequested ? "true" : "false")
                .keyName("file")
                .build();
    }

    @Override
    public String getResourceName() {
        return targetFileName;
    }

    public static class Builder extends ProvisionedResource.Builder<FolderResourceDefinitionProvisioned, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder targetFolderName(String targetFolderName) {
            provisionedResource.targetFolderName = targetFolderName;
            return this;
        }

        public Builder targetFileName(String targetFileName) {
            provisionedResource.targetFileName = targetFileName;
            return this;
        }

        public Builder isCompressRequested(boolean isCompressRequested) {
            provisionedResource.isCompressRequested = isCompressRequested;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(provisionedResource.targetFolderName, "targetFolderName");
            Objects.requireNonNull(provisionedResource.targetFileName, "targetFileName");
        }

        private Builder() {
            super(new FolderResourceDefinitionProvisioned());
        }
    }
}
