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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A provisioned resource that is asset content.
 *
 * This resource type is created when a provider's backend system provisions data as part of a data transfer.
 */
public abstract class ProvisionedContentResource extends ProvisionedDataAddressResource {

    protected ProvisionedContentResource() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<T extends ProvisionedContentResource, B extends Builder<T, B>> extends ProvisionedDataAddressResource.Builder<T, B> {

        protected Builder(T resource) {
            super(resource);
        }

    }
}
