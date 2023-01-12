/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.edc.connector.provision.gcp;

import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Objects;

public class GcsResourceDefinition extends ResourceDefinition {

    private DataAddress dataAddress;
    private String location;

    private String projectId;
    private String storageClass;


    private GcsResourceDefinition() {
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getLocation() {
        return this.location;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public String getStorageClass() {
        return this.storageClass;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .dataAddress(dataAddress)
                .location(location)
                .projectId(projectId)
                .storageClass(storageClass);
    }

    public static class Builder extends ResourceDefinition.Builder<GcsResourceDefinition, Builder> {

        private Builder() {
            super(new GcsResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            resourceDefinition.dataAddress = dataAddress;
            return this;
        }

        public Builder location(String location) {
            resourceDefinition.location = location;
            return this;
        }

        public Builder projectId(String projectId) {
            resourceDefinition.projectId = projectId;
            return this;
        }

        public Builder storageClass(String storageClass) {
            resourceDefinition.storageClass = storageClass;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.location, "location");
            Objects.requireNonNull(resourceDefinition.storageClass, "storageClass");
        }
    }
}
