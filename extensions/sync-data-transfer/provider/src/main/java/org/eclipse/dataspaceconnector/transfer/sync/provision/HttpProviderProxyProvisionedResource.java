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

package org.eclipse.dataspaceconnector.transfer.sync.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;

import java.util.UUID;

import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.AUTH_CODE;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.AUTH_KEY;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.EXPIRATION;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.TYPE;

public class HttpProviderProxyProvisionedResource extends ProvisionedDataDestinationResource {

    @JsonProperty
    private String address;
    @JsonProperty
    private String authKey;
    @JsonProperty
    private String authCode;
    @JsonProperty
    private long expirationEpochSeconds;

    private HttpProviderProxyProvisionedResource() {
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, address)
                .property(AUTH_KEY, authKey)
                .property(AUTH_CODE, authCode)
                .property(EXPIRATION, String.valueOf(expirationEpochSeconds))
                .build();
    }

    @Override
    public String getResourceName() {
        return UUID.randomUUID().toString();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedResource.Builder<HttpProviderProxyProvisionedResource, Builder> {

        private Builder() {
            super(new HttpProviderProxyProvisionedResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder address(String address) {
            provisionedResource.address = address;
            return this;
        }

        public Builder authKey(String authKey) {
            provisionedResource.authKey = authKey;
            return this;
        }

        public Builder authCode(String authCode) {
            provisionedResource.authCode = authCode;
            return this;
        }

        public Builder expirationEpochSeconds(long expirationEpochSeconds) {
            provisionedResource.expirationEpochSeconds = expirationEpochSeconds;
            return this;
        }
    }
}
