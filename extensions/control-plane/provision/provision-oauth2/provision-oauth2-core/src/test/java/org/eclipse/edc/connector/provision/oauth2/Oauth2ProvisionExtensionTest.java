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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
class Oauth2ProvisionExtensionTest {

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new TestExtension());
    }

    @Test
    void generateProviderManifest(ResourceManifestGenerator manifestGenerator) {
        var oauth2DataAddress = validOauth2DataAddress();
        var oauth2HttpDataRequest = DataRequest.Builder.newInstance().id(UUID.randomUUID().toString()).destinationType("any").build();

        var resourceManifest = manifestGenerator.generateProviderResourceManifest(oauth2HttpDataRequest, oauth2DataAddress, Policy.Builder.newInstance().build());

        assertThat(resourceManifest.getDefinitions()).hasSize(1);
    }

    @Test
    void generateConsumerManifest(ResourceManifestGenerator manifestGenerator) {
        var oauth2DataAddress = validOauth2DataAddress();
        var oauth2HttpDataRequest = DataRequest.Builder.newInstance().id(UUID.randomUUID().toString()).dataDestination(oauth2DataAddress).build();

        var resourceManifest = manifestGenerator.generateConsumerResourceManifest(oauth2HttpDataRequest, Policy.Builder.newInstance().build());

        assertThat(resourceManifest.succeeded()).isTrue();
        assertThat(resourceManifest.getContent().getDefinitions()).hasSize(1);
    }

    @Test
    void oauth2Provisioner(ProvisionManager provisionManager) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "any")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "any")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "http://any/url")
                .build();
        var resourceDefinition = Oauth2ResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .dataAddress(dataAddress)
                .build();

        var future = provisionManager.provision(List.of(resourceDefinition), Policy.Builder.newInstance().build());

        assertThat(future).succeedsWithin(10, SECONDS).asList().hasSize(1)
                .first().asInstanceOf(InstanceOfAssertFactories.type(StatusResult.class)).matches(StatusResult::succeeded)
                .extracting(StatusResult::getContent).asInstanceOf(type(ProvisionResponse.class))
                .extracting(ProvisionResponse::getSecretToken).isNotNull();
    }

    private DataAddress validOauth2DataAddress() {
        return HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "clientId")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "clientSecret")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "tokenUrl")
                .build();
    }

    public static class TestExtension implements ServiceExtension {
        @Provider
        public Oauth2Client oauth2Client() {
            var client = mock(Oauth2Client.class);
            when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
            return client;
        }
    }
}
