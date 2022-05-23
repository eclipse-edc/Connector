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

@JsonDeserialize(builder = DestinationUrlProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:destinationurlprovisionedresource")
public class DestinationUrlProvisionedResource extends ProvisionedContentResource {
    private String url;

    private DestinationUrlProvisionedResource() {
        super();
    }

    public String getUrl() {
        return url;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedContentResource.Builder<DestinationUrlProvisionedResource, Builder> {

        private Builder() {
            super(new DestinationUrlProvisionedResource());
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

        public Builder url(String url) {
            provisionedResource.url = url;
            return this;
        }
    }
}
