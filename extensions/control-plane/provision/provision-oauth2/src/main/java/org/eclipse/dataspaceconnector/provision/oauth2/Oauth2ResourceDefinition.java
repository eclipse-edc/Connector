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

package org.eclipse.dataspaceconnector.provision.oauth2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import static org.eclipse.dataspaceconnector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.dataspaceconnector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_SECRET;
import static org.eclipse.dataspaceconnector.provision.oauth2.Oauth2DataAddressSchema.TOKEN_URL;

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

    public String getClientId() {
        return dataAddress.getProperty(CLIENT_ID);
    }

    public String getClientSecret() {
        return dataAddress.getProperty(CLIENT_SECRET);
    }

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
