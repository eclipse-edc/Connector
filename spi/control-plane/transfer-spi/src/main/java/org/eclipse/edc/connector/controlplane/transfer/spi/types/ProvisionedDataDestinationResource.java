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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A provisioned resource that serves as a data destination.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:provisioneddatadestinationresource")
@JsonDeserialize(builder = ProvisionedDataDestinationResource.Builder.class)
public abstract class ProvisionedDataDestinationResource extends ProvisionedDataAddressResource {

    protected ProvisionedDataDestinationResource() {
        super();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<T extends ProvisionedDataDestinationResource, B extends ProvisionedDataDestinationResource.Builder<T, B>> extends ProvisionedDataAddressResource.Builder<T, B> {

        protected Builder(T resource) {
            super(resource);
        }

    }
}
