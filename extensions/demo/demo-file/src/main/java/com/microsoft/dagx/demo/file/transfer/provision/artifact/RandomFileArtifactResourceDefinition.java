package com.microsoft.dagx.demo.file.transfer.provision.artifact;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

public class RandomFileArtifactResourceDefinition extends ResourceDefinition {
    private String targetFolderName;
    private String targetFileName;
    private boolean isCompressionRequested = false;

    private RandomFileArtifactResourceDefinition() {
    }

    public String getTargetFolderName() {
        return targetFolderName;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public boolean isCompressionRequested() {
        return isCompressionRequested;
    }

    public static class Builder extends ResourceDefinition.Builder<RandomFileArtifactResourceDefinition, RandomFileArtifactResourceDefinition.Builder> {

        public static RandomFileArtifactResourceDefinition.Builder newInstance() {
            return new RandomFileArtifactResourceDefinition.Builder();
        }

        public RandomFileArtifactResourceDefinition.Builder targetFolderName(String folderName) {
            resourceDefinition.targetFolderName = folderName;
            return this;
        }

        public RandomFileArtifactResourceDefinition.Builder targetFileName(String fileName) {
            resourceDefinition.targetFileName = fileName;
            return this;
        }

        public RandomFileArtifactResourceDefinition.Builder isCompressionRequested(boolean isCompressionRequested) {
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
            super(new RandomFileArtifactResourceDefinition());
        }
    }
}
