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

package org.eclipse.dataspaceconnector.gcp.storage.provision;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

public class GcsResourceDefinition extends ResourceDefinition {

    private String location;
    private String storageClass;

    private GcsResourceDefinition() {
    }

    public String getLocation() {
        return this.location;
    }

    public String getStorageClass() {
        return this.storageClass;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .location(location)
                .storageClass(storageClass);
    }

    public static class Builder extends ResourceDefinition.Builder<GcsResourceDefinition, Builder> {

        private Builder() {
            super(new GcsResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder location(String location) {
            resourceDefinition.location = location;
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
