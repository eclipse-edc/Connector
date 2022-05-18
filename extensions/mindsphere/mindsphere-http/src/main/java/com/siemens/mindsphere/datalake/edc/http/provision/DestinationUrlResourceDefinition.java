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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package com.siemens.mindsphere.datalake.edc.http.provision;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

public class DestinationUrlResourceDefinition extends ResourceDefinition {
    private String path;

    private DestinationUrlResourceDefinition() {
    }

    public String getPath() {
        return path;
    }

    public static class Builder extends ResourceDefinition.Builder<DestinationUrlResourceDefinition, Builder> {

        private Builder() {
            super(new DestinationUrlResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder path(String path) {
            resourceDefinition.path = path;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.path, "path");
        }
    }
}
