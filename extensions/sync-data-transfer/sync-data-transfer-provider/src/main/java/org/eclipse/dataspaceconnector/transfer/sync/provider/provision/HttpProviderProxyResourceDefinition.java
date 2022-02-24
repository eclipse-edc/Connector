/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.sync.provider.provision;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

/**
 * Definition of a Http proxy serving data on provider side.
 */
public class HttpProviderProxyResourceDefinition extends ResourceDefinition {
    private String contractId;
    private String assetId;

    private HttpProviderProxyResourceDefinition() {
    }

    public String getContractId() {
        return contractId;
    }


    public String getAssetId() {
        return assetId;
    }

    public static class Builder extends ResourceDefinition.Builder<HttpProviderProxyResourceDefinition, Builder> {

        private Builder() {
            super(new HttpProviderProxyResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contractId(String contractId) {
            resourceDefinition.contractId = contractId;
            return this;
        }

        public Builder assetId(String assetId) {
            resourceDefinition.assetId = assetId;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.contractId, "contractId");
            Objects.requireNonNull(resourceDefinition.assetId, "assetId");
        }
    }
}
