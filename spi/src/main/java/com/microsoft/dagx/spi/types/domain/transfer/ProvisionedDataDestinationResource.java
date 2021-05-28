/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import java.util.function.Supplier;

/**
 * A provisioned resource that serves as a data destination.
 */
public abstract class ProvisionedDataDestinationResource extends ProvisionedResource {

    protected ProvisionedDataDestinationResource() {
        super();
    }

    public abstract DataAddress createDataDestination();

    public abstract String getResourceName();


    public abstract Supplier<Boolean> getCompletionChecker();

    protected static class Builder<PR extends ProvisionedResource, B extends ProvisionedResource.Builder<PR, B>> extends ProvisionedResource.Builder<PR, B> {

        protected Builder(PR resource) {
            super(resource);
        }

    }
}
