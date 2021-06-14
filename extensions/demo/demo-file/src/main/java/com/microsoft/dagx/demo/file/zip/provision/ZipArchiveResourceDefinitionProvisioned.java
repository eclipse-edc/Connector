/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file.zip.provision;

import com.microsoft.dagx.demo.file.zip.schema.ZipSchema;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

import java.util.Objects;


public class ZipArchiveResourceDefinitionProvisioned extends ProvisionedDataDestinationResource {

    private String directory;
    private String zipFileName;

    private ZipArchiveResourceDefinitionProvisioned() {
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .type(ZipSchema.TYPE)
                .property(ZipSchema.NAME, zipFileName)
                .property(ZipSchema.DIRECTORY, directory)
                .keyName("password")
                .build();
    }

    @Override
    public String getResourceName() {
        return ZipArchiveResourceDefinitionProvisioned.class.getName();
    }

    public static class Builder extends ProvisionedResource.Builder<ZipArchiveResourceDefinitionProvisioned, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder directory(String directory) {
            provisionedResource.directory = directory;
            return this;
        }

        public Builder zipFileName(String zipFileName) {
            provisionedResource.zipFileName = zipFileName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(provisionedResource.directory, "directory");
            Objects.requireNonNull(provisionedResource.zipFileName, "zipFileName");
        }

        private Builder() {
            super(new ZipArchiveResourceDefinitionProvisioned());
        }
    }
}
