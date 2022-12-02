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

import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.mockserver.model.ParameterBody;
import org.mockserver.model.Parameters;

import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.MediaType.APPLICATION_JSON;

class Oauth2ProvisionerTest {

    private final int port = getFreePort();
    private final ClientAndServer server = startClientAndServer(port);
    private final EdcHttpClient httpClient = testHttpClient();

    private final TypeManager typeManager = new TypeManager();
    private final Oauth2Provisioner provisioner = new Oauth2Provisioner(httpClient, typeManager);

    @Test
    void canProvisionOauth2ResourceDefinition() {
        assertThat(provisioner.canProvision(mock(Oauth2ResourceDefinition.class))).isTrue();
        assertThat(provisioner.canProvision(mock(ResourceDefinition.class))).isFalse();
    }

    @Test
    void canDeprovisionOauth2ResourceDefinition() {
        assertThat(provisioner.canDeprovision(mock(Oauth2ProvisionedResource.class))).isTrue();
        assertThat(provisioner.canDeprovision(mock(ProvisionedResource.class))).isFalse();
    }

    @Test
    void provisionRequestOauth2TokenAndReturnsIt() {
        var formParameters = Parameters.parameters(
                Parameter.param("grant_type", "client_credentials"),
                Parameter.param("client_id", "clientId"),
                Parameter.param("client_secret", "clientSecret")
        );
        var expectedRequest = HttpRequest.request().withBody(new ParameterBody(formParameters));
        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token"));
        server.when(expectedRequest).respond(HttpResponse.response().withBody(responseBody, APPLICATION_JSON));

        var resourceDefinitionId = UUID.randomUUID().toString();
        var transferProcessId = UUID.randomUUID().toString();
        var resource = createResourceDefinition(resourceDefinitionId, transferProcessId);

        var future = provisioner.provision(resource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::succeeded)
                .extracting(AbstractResult::getContent)
                .satisfies(provisionResponse -> {
                    assertThat(provisionResponse.getResource()).asInstanceOf(type(Oauth2ProvisionedResource.class))
                            .satisfies(resourceDefinition -> {
                                assertThat(resourceDefinition.getResourceDefinitionId()).isEqualTo(resourceDefinitionId);
                                assertThat(resourceDefinition.getTransferProcessId()).isEqualTo(transferProcessId);
                                assertThat(resourceDefinition.hasToken()).isTrue();
                                assertThat(resourceDefinition.getResourceName()).endsWith("-oauth2");
                                assertThat(resourceDefinition.getDataAddress())
                                        .extracting(it -> it.getProperty("secretName")).asString().endsWith("-oauth2");
                            });
                    assertThat(provisionResponse.getSecretToken()).asInstanceOf(type(Oauth2SecretToken.class))
                            .extracting(Oauth2SecretToken::getToken).isEqualTo("Bearer token");
                });
        server.verify(expectedRequest);
    }

    @Test
    void provisionReturnFailureIfServerCallFails() {
        server.when(HttpRequest.request()).respond(HttpResponse.response().withStatusCode(400));

        var resourceDefinitionId = UUID.randomUUID().toString();
        var transferProcessId = UUID.randomUUID().toString();
        var resource = createResourceDefinition(resourceDefinitionId, transferProcessId);

        var future = provisioner.provision(resource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::failed)
                .extracting(AbstractResult::getFailure)
                .asInstanceOf(type(ResponseFailure.class))
                .matches(it -> it.status() == FATAL_ERROR);
    }

    @Test
    void provisionReturnFailureIfServerIsNotReachable() {
        server.stop();

        var resourceDefinitionId = UUID.randomUUID().toString();
        var transferProcessId = UUID.randomUUID().toString();
        var resource = createResourceDefinition(resourceDefinitionId, transferProcessId);

        var future = provisioner.provision(resource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::failed)
                .extracting(AbstractResult::getFailure)
                .asInstanceOf(type(ResponseFailure.class))
                .matches(it -> it.status() == FATAL_ERROR);
    }

    @Test
    void deprovisioningDoesNothingAsTheTokenWillExpireAtCertainPoint() {
        var provisionedResourceId = UUID.randomUUID().toString();
        var provisionedResource = Oauth2ProvisionedResource.Builder.newInstance()
                .id(provisionedResourceId)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .resourceName("any")
                .dataAddress(createDataAddress())
                .hasToken(true)
                .build();

        var future = provisioner.deprovision(provisionedResource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::succeeded)
                .extracting(AbstractResult::getContent).asInstanceOf(type(DeprovisionedResource.class))
                .satisfies(deprovisioned -> {
                    assertThat(deprovisioned.getProvisionedResourceId()).isEqualTo(provisionedResourceId);
                });
    }

    private Oauth2ResourceDefinition createResourceDefinition(String resourceDefinitionId, String transferProcessId) {
        return Oauth2ResourceDefinition.Builder.newInstance()
                .id(resourceDefinitionId)
                .transferProcessId(transferProcessId)
                .dataAddress(createDataAddress())
                .build();
    }

    private DataAddress createDataAddress() {
        return HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "clientId")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "clientSecret")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "http://localhost:" + port)
                .build();
    }

    private Policy simplePolicy() {
        return Policy.Builder.newInstance().build();
    }
}
