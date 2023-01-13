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
 *       Amadeus - use Oauth2Client
 *
 */

package org.eclipse.edc.connector.provision.oauth2;

import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Oauth2ProvisionerTest {

    private final Oauth2Client client = mock(Oauth2Client.class);
    private final Oauth2CredentialsRequestFactory requestFactory = mock(Oauth2CredentialsRequestFactory.class);
    private final Oauth2Provisioner provisioner = new Oauth2Provisioner(client, requestFactory);

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
        when(requestFactory.create(any())).thenReturn(Result.success(createRequest()));
        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token-test").build()));
        var resource = createResourceDefinition();

        var future = provisioner.provision(resource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::succeeded)
                .extracting(AbstractResult::getContent)
                .satisfies(provisionResponse -> {
                    assertThat(provisionResponse.getResource()).asInstanceOf(type(Oauth2ProvisionedResource.class))
                            .satisfies(resourceDefinition -> {
                                assertThat(resourceDefinition.getResourceDefinitionId()).isEqualTo(resource.getId());
                                assertThat(resourceDefinition.getTransferProcessId()).isEqualTo(resource.getTransferProcessId());
                                assertThat(resourceDefinition.hasToken()).isTrue();
                                assertThat(resourceDefinition.getResourceName()).endsWith("-oauth2");
                                assertThat(resourceDefinition.getDataAddress())
                                        .extracting(it -> it.getProperty("secretName")).asString().endsWith("-oauth2");
                            });
                    assertThat(provisionResponse.getSecretToken()).asInstanceOf(type(Oauth2SecretToken.class))
                            .extracting(Oauth2SecretToken::getToken).isEqualTo("Bearer token-test");
                });
    }

    @Test
    void provisionRequestReturnsFailureIfRequestCannotBeCreated() {
        when(requestFactory.create(any())).thenReturn(Result.failure("error"));

        var future = provisioner.provision(createResourceDefinition(), simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StatusResult::failed)
                .extracting(StatusResult::getFailure)
                .satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(FATAL_ERROR);
                    assertThat(failure.getFailureDetail()).contains("error");
                });

        verifyNoInteractions(client);
    }

    @Test
    void provisionReturnFailureIfServerCallFails() {
        when(requestFactory.create(any())).thenReturn(Result.success(createRequest()));
        when(client.requestToken(any())).thenReturn(Result.failure("error test"));

        var future = provisioner.provision(createResourceDefinition(), simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StatusResult::failed)
                .extracting(AbstractResult::getFailure)
                .satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(FATAL_ERROR);
                    assertThat(failure.getFailureDetail()).contains("error test");
                });
    }

    @Test
    void deprovisioningDoesNothingAsTheTokenWillExpireAtCertainPoint() {
        var provisionedResourceId = UUID.randomUUID().toString();
        var provisionedResource = Oauth2ProvisionedResource.Builder.newInstance()
                .id(provisionedResourceId)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .resourceName("any")
                .dataAddress(HttpDataAddress.Builder.newInstance().build())
                .hasToken(true)
                .build();

        var future = provisioner.deprovision(provisionedResource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::succeeded)
                .extracting(AbstractResult::getContent).asInstanceOf(type(DeprovisionedResource.class))
                .extracting(DeprovisionedResource::getProvisionedResourceId)
                .isEqualTo(provisionedResourceId);
    }

    private SharedSecretOauth2CredentialsRequest createRequest() {
        return SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url("http://any")
                .grantType("any")
                .clientId("any").clientSecret("any").build();
    }

    private Oauth2ResourceDefinition createResourceDefinition() {
        return Oauth2ResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .build();
    }

    private Policy simplePolicy() {
        return Policy.Builder.newInstance().build();
    }
}
