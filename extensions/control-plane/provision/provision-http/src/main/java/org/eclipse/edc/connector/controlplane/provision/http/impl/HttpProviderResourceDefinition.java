/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - add toBuilder method
 *
 */

package org.eclipse.edc.connector.controlplane.provision.http.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static java.util.Objects.requireNonNull;

/**
 * A resource to be provisioned by an external HTTP endpoint.
 */
@JsonTypeName("dataspaceconnector:httpproviderresourcedefinition")
@JsonDeserialize(builder = HttpProviderResourceDefinition.Builder.class)
public class HttpProviderResourceDefinition extends AbstractHttpResourceDefinition {
    private String assetId;

    private HttpProviderResourceDefinition() {
        super();
    }

    public String getAssetId() {
        return assetId;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .assetId(assetId);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends AbstractHttpResourceDefinition.Builder<HttpProviderResourceDefinition, Builder> {

        private Builder() {
            super(new HttpProviderResourceDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            resourceDefinition.assetId = assetId;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            requireNonNull(resourceDefinition.assetId, "assetId");
        }

    }

}
