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

package org.eclipse.dataspaceconnector.transfer.provision.http;

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

    public String getAssetId() {
        return assetId;
    }

    private HttpProvisionedContentResource() {
        super();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedContentResource.Builder<HttpProvisionedContentResource, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder transferProcessId(String transferProcessId) {
            provisionedResource.transferProcessId = transferProcessId;
            return this;
        }

        public Builder assetId(String assetId) {
            provisionedResource.assetId = assetId;
            return this;
        }

        private Builder() {
            super(new HttpProvisionedContentResource());
        }
    }
}
