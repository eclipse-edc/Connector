/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

/**
 * Defines a topic to be provisioned that will receive push data from the provider runtime.
 */
public class PushStreamResourceDefinition extends ResourceDefinition {
    private String endpointAddress;
    private String topicName;

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public String getTopicName() {
        return topicName;
    }

    public static class Builder extends ResourceDefinition.Builder<PushStreamResourceDefinition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpointAddress(String endpointAddress) {
            resourceDefinition.endpointAddress = endpointAddress;
            return this;
        }

        public Builder topicName(String topicName) {
            resourceDefinition.topicName = topicName;
            return this;
        }

        public void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.endpointAddress, "endpointAddress");
            Objects.requireNonNull(resourceDefinition.topicName, "topicName");
        }
        private Builder() {
            super(new PushStreamResourceDefinition());
        }
    }

}
