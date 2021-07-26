/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A provisioned resource that serves as a data destination.
 */
@JsonTypeName("dagx:provisioneddatadestinationresource")
@JsonDeserialize(builder = ProvisionedDataDestinationResource.Builder.class)
public abstract class ProvisionedDataDestinationResource extends ProvisionedResource {

    protected ProvisionedDataDestinationResource() {
        super();
    }

    public abstract DataAddress createDataDestination();

    public abstract String getResourceName();

    @JsonPOJOBuilder(withPrefix = "")
    protected static class Builder<PR extends ProvisionedResource, B extends ProvisionedResource.Builder<PR, B>> extends ProvisionedResource.Builder<PR, B> {

        protected Builder(PR resource) {
            super(resource);
        }

    }
}
