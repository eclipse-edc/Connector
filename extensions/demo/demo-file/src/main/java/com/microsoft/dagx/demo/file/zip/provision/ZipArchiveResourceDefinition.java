/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file.zip.provision;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

public class ZipArchiveResourceDefinition extends ResourceDefinition {

    private String directory;
    private String zipFileName;

    private ZipArchiveResourceDefinition() {
    }

    public String getDirectory() {
        return directory;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public static class Builder extends ResourceDefinition.Builder<ZipArchiveResourceDefinition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder directory(String targetFolderName) {
            resourceDefinition.directory = targetFolderName;
            return this;
        }

        public Builder zipFileName(String zipFileName) {
            resourceDefinition.zipFileName = zipFileName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.directory, "directory");
            Objects.requireNonNull(resourceDefinition.zipFileName, "zipFileName");
        }

        private Builder() {
            super(new ZipArchiveResourceDefinition());
        }
    }
}
