/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

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
