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
 *
 */

package org.eclipse.dataspaceconnector.transfer.provision.http.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;

/**
 * A reference to a provisioned resource.
 */
@JsonDeserialize(builder = HttpProvisionedContentResource.Builder.class)
@JsonTypeName("dataspaceconnector:httpprovisionedresource")
public class HttpProvisionedContentResource extends ProvisionedContentResource {
    private String assetId;

    private HttpProvisionedContentResource() {
        super();
    }

    public String getAssetId() {
        return assetId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedContentResource.Builder<HttpProvisionedContentResource, Builder> {

        private Builder() {
            super(new HttpProvisionedContentResource());
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

        public Builder assetId(String assetId) {
            provisionedResource.assetId = assetId;
            return this;
        }
    }
}
