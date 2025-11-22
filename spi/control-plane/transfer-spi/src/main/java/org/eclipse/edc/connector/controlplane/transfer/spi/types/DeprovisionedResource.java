/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Microsoft Corporation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static java.util.Objects.requireNonNull;

/**
 * A response for a deprovision operation.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:deprovisionedresource")
@JsonDeserialize(builder = DeprovisionedResource.Builder.class)
public class DeprovisionedResource {
    private String provisionedResourceId;
    private boolean inProcess;
    private boolean error;
    private String errorMessage;

    private DeprovisionedResource() {
    }

    public String getProvisionedResourceId() {
        return provisionedResourceId;
    }

    @JsonProperty("error")
    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isInProcess() {
        return inProcess;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DeprovisionedResource resource;

        private Builder() {
            resource = new DeprovisionedResource();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder provisionedResourceId(String id) {
            resource.provisionedResourceId = id;
            return this;
        }

        public Builder errorMessage(String message) {
            resource.errorMessage = message;
            if (message != null) {
                resource.error = true;
            }
            return this;
        }

        @JsonProperty("inProcess")
        public Builder inProcess(boolean value) {
            resource.inProcess = value;
            return this;
        }

        /**
         * Method for deserialization.
         */
        @JsonProperty("error")
        Builder error(boolean value) {
            resource.error = value;
            return this;
        }

        public DeprovisionedResource build() {
            requireNonNull(resource.provisionedResourceId, "provisionedResourceId");
            return resource;
        }

    }
}
