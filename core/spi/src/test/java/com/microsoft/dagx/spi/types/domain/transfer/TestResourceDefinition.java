/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonTypeName("dagx:testresourcedefinition")
@JsonDeserialize(builder = TestResourceDefinition.Builder.class)
class TestResourceDefinition extends ResourceDefinition {

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
