/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

/**
 * A provisioned resource that serves as a data destination.
 */
@JsonTypeName("dataspaceconnector:provisioneddatadestinationresource")
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
