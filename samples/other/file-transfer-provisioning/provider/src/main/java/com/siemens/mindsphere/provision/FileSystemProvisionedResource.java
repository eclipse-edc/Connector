/*
 *  Copyright (c) 2021, 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *
 */

package com.siemens.mindsphere.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;

@JsonDeserialize(builder = FileSystemProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:filesystemprovisionedresource")
public class FileSystemProvisionedResource extends ProvisionedContentResource {
    private String path;

    private FileSystemProvisionedResource() {
        super();
    }

    public String getPath() {
        return path;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedContentResource.Builder<FileSystemProvisionedResource, Builder> {

        private Builder() {
            super(new FileSystemProvisionedResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder transferProcessId(String transferProcessId) {
            provisionedResource.transferProcessId = transferProcessId;
            return this;
        }

        public Builder path(String path) {
            provisionedResource.path = path;
            return this;
        }
    }
}
