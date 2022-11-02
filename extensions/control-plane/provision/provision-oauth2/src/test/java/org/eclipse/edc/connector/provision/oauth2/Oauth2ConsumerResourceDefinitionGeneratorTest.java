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

import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class Oauth2ConsumerResourceDefinitionGeneratorTest {

    private final ConsumerResourceDefinitionGenerator generator = new Oauth2ConsumerResourceDefinitionGenerator();

    @Test
    void returnsNullIfDoesNotHaveOauth2Parameters() {
        var dataRequest = DataRequest.Builder.newInstance().id(UUID.randomUUID().toString()).destinationType("HttpData").build();

        var definition = generator.generate(dataRequest, simplePolicy());

        assertThat(definition).isNull();
    }

    @Test
    void returnDefinitionIfTypeIsHttpDataAndOauth2ParametersArePresent() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                .build();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataDestination(dataAddress)
                .build();

        var definition = generator.generate(dataRequest, simplePolicy());

        assertThat(definition).isNotNull().asInstanceOf(type(Oauth2ResourceDefinition.class))
                .satisfies(d -> {
                    assertThat(d.getClientId()).isEqualTo("aClientId");
                    assertThat(d.getClientSecret()).isEqualTo("aSecret");
                    assertThat(d.getTokenUrl()).isEqualTo("aTokenUrl");
                });
    }

    private Policy simplePolicy() {
        return Policy.Builder.newInstance().build();
    }
}
