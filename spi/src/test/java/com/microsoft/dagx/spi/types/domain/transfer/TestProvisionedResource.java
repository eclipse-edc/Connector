/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = TestProvisionedResource.Builder.class)
@JsonTypeName("dagx:testprovisionedresource")
class TestProvisionedResource extends ProvisionedResource {

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedResource.Builder<TestProvisionedResource, Builder> {
        private Builder() {
            super(new TestProvisionedResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
