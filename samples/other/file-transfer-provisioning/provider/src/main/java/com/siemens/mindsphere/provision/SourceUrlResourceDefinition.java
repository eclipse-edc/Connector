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

public class SourceUrlResourceDefinition extends ResourceDefinition {
    private String url;

    private SourceUrlResourceDefinition() {
    }

    public String getUrl() {
        return url;
    }

    public static class Builder extends ResourceDefinition.Builder<SourceUrlResourceDefinition, Builder> {

        private Builder() {
            super(new SourceUrlResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder url(String url) {
            resourceDefinition.url = url;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.url, "url");
        }
    }
}
