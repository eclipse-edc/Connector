/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.transfer.provision.fixtures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;

@JsonTypeName("dataspaceconnector:testresourcedefinition")
@JsonDeserialize(builder = TestResourceDefinition.Builder.class)
public class TestResourceDefinition extends ResourceDefinition {

    @Override
    public <RD extends ResourceDefinition, B extends ResourceDefinition.Builder<RD, B>> B toBuilder() {
        return null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ResourceDefinition.Builder<TestResourceDefinition, Builder> {
        private Builder() {
            super(new TestResourceDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
