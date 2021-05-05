/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

/**
 *
 */
class TestProvisionedResource extends ProvisionedResource {

    public static class Builder extends ProvisionedResource.Builder<TestProvisionedResource, Builder> {
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TestProvisionedResource());
        }
    }
}
