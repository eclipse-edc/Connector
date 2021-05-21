/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

/**
 * Defines a destination to be provisioned that will receive push data events from the provider connector.
 */
public class PushStreamResourceDefinition extends ResourceDefinition {
    private String endpointAddress;
    private String destinationName;

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public static class Builder extends ResourceDefinition.Builder<PushStreamResourceDefinition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpointAddress(String endpointAddress) {
            resourceDefinition.endpointAddress = endpointAddress;
            return this;
        }

        public Builder destinationName(String destinationName) {
            resourceDefinition.destinationName = destinationName;
            return this;
        }

        public void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.endpointAddress, "endpointAddress");
            Objects.requireNonNull(resourceDefinition.destinationName, "destinationName");
        }
        private Builder() {
            super(new PushStreamResourceDefinition());
        }
    }

}
