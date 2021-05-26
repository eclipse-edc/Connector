package com.microsoft.dagx.demo.file.transfer.provision.folder;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

public class FolderResourceDefinition extends ResourceDefinition {

    private String targetFolderName;
    private String targetFileName;
    private boolean isCompressionRequested;

    private FolderResourceDefinition() {
    }

    public String getTargetFolderName() {
        return targetFolderName;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public boolean getIsCompressionRequested() {
        return isCompressionRequested;
    }

    public static class Builder extends ResourceDefinition.Builder<FolderResourceDefinition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder targetFolderName(String targetFolderName) {
            resourceDefinition.targetFolderName = targetFolderName;
            return this;
        }

        public Builder targetFileName(String targetFileName) {
            resourceDefinition.targetFileName = targetFileName;
            return this;
        }

        public Builder isCompressionRequested(boolean isCompressionRequested) {
            resourceDefinition.isCompressionRequested = isCompressionRequested;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.targetFolderName, "targetFolderName");
            Objects.requireNonNull(resourceDefinition.targetFileName, "targetFileName");
        }

        private Builder() {
            super(new FolderResourceDefinition());
        }
    }
}
