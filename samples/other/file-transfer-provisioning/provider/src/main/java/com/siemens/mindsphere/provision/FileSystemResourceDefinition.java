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

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

public class FileSystemResourceDefinition extends ResourceDefinition {
    private String path;

    private FileSystemResourceDefinition() {
    }

    public String getPath() {
        return path;
    }

    public static class Builder extends ResourceDefinition.Builder<FileSystemResourceDefinition, Builder> {

        private Builder() {
            super(new FileSystemResourceDefinition());
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
