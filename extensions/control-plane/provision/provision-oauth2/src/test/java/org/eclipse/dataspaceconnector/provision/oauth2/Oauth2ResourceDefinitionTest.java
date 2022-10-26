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

import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.dataspaceconnector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_SECRET;
import static org.eclipse.dataspaceconnector.provision.oauth2.Oauth2DataAddressSchema.TOKEN_URL;

class Oauth2ResourceDefinitionTest {

    @Test
    void serdes() {
        var typeManager = new TypeManager();
        var address = HttpDataAddress.Builder.newInstance()
                .property(CLIENT_ID, "clientId")
                .property(CLIENT_SECRET, "clientSecret")
                .property(TOKEN_URL, "http://any/url")
                .build();
        var resourceDefinition = Oauth2ResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .dataAddress(address)
                .build();

        var json = typeManager.writeValueAsString(resourceDefinition);
        var deserialized = typeManager.readValue(json, Oauth2ResourceDefinition.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(resourceDefinition);
    }
}
