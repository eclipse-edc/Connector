/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

/**
 *
 */
public class ObjectStorageResourceDefinition extends ResourceDefinition {


    public static class Builder extends ResourceDefinition.Builder<ObjectStorageResourceDefinition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ObjectStorageResourceDefinition());
        }
    }

}
