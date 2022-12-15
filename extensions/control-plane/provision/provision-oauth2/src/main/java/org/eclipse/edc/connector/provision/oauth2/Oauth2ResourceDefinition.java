/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.oauth2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_SECRET;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.PRIVATE_KEY_NAME;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.TOKEN_URL;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.VALIDITY;

/**
 * An OAuth2 resource definition
 */
@JsonDeserialize(builder = Oauth2ResourceDefinition.Builder.class)
@JsonTypeName("dataspaceconnector:oauth2resourcedefinition")
public class Oauth2ResourceDefinition extends ResourceDefinition {

    private DataAddress dataAddress;

    private Oauth2ResourceDefinition() {
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder()).dataAddress(dataAddress);
    }

    @NotNull
    public String getClientId() {
        return dataAddress.getProperty(CLIENT_ID);
    }

    @Nullable
    public String getClientSecret() {
        return dataAddress.getProperty(CLIENT_SECRET);
    }

    @Nullable
    public String getPrivateKeyName() {
        return dataAddress.getProperty(PRIVATE_KEY_NAME);
    }

    @Nullable
    public Long getValidity() {
        return Optional.ofNullable(dataAddress.getProperty(VALIDITY))
                .map(Long::parseLong)
                .orElse(null);
    }

    @NotNull
    public String getTokenUrl() {
        return dataAddress.getProperty(TOKEN_URL);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ResourceDefinition.Builder<Oauth2ResourceDefinition, Builder> {

        private Builder() {
            super(new Oauth2ResourceDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            this.resourceDefinition.dataAddress = dataAddress;
            return this;
        }
    }
}
