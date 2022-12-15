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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.PrivateKey;
import java.sql.Date;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Oauth2ProvisionerTest {

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private final Oauth2Client client = mock(Oauth2Client.class);
    private final PrivateKeyResolver privateKeyResolver = mock(PrivateKeyResolver.class);
    private final Oauth2Provisioner provisioner = new Oauth2Provisioner(client, privateKeyResolver, clock);

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
    void provisionRequestOauth2TokenWithSharedSecretAndReturnsIt() {
        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token-test").build()));

        var address = createDataAddressWithSharedSecret();
        var resourceDefinitionId = UUID.randomUUID().toString();
        var transferProcessId = UUID.randomUUID().toString();
        var resource = createResourceDefinition(address, resourceDefinitionId, transferProcessId);

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
                            .extracting(Oauth2SecretToken::getToken).isEqualTo("Bearer token-test");
                });

        var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
        verify(client).requestToken(captor.capture());
        var captured = captor.getValue();
        assertThat(captured)
                .isNotNull()
                .isInstanceOf(SharedSecretOauth2CredentialsRequest.class);
        var request = (SharedSecretOauth2CredentialsRequest) captured;
        assertThat(request.getGrantType()).isEqualTo("client_credentials");
        assertThat(request.getClientId()).isEqualTo("clientId");
        assertThat(request.getClientSecret()).isEqualTo("clientSecret");
        assertThat(request.getUrl()).isEqualTo("http://oauth2-server.com:");
        verifyNoInteractions(privateKeyResolver);
    }

    @Test
    void provisionRequestOauth2TokenWithPrivateKeyKeyAndReturnsIt() throws JOSEException, ParseException {
        var keyPair = generateKeyPair();
        when(privateKeyResolver.resolvePrivateKey("pk-test", PrivateKey.class)).thenReturn(keyPair.toPrivateKey());
        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token-test").build()));

        var address = createDataAddressWithPrivateKey();
        var resourceDefinitionId = UUID.randomUUID().toString();
        var transferProcessId = UUID.randomUUID().toString();
        var resource = createResourceDefinition(address, resourceDefinitionId, transferProcessId);

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
                            .extracting(Oauth2SecretToken::getToken).isEqualTo("Bearer token-test");
                });

        var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
        verify(client).requestToken(captor.capture());
        var captured = captor.getValue();
        assertThat(captured)
                .isNotNull()
                .isInstanceOf(PrivateKeyOauth2CredentialsRequest.class);
        var request = (PrivateKeyOauth2CredentialsRequest) captured;
        assertThat(request.getGrantType()).isEqualTo("client_credentials");
        assertThat(request.getUrl()).isEqualTo("http://oauth2-server.com:");
        assertThat(request.getClientAssertionType()).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

        var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var assertionToken = SignedJWT.parse(request.getClientAssertion());
        assertThat(assertionToken.verify(new RSASSAVerifier(keyPair.toRSAPublicKey()))).isTrue();
        assertThat(assertionToken.getJWTClaimsSet().getClaims())
                .hasFieldOrPropertyWithValue("sub", "clientId")
                .hasFieldOrPropertyWithValue("iss", "clientId")
                .hasFieldOrPropertyWithValue("aud", List.of(address.getProperty(Oauth2DataAddressSchema.TOKEN_URL)))
                .hasFieldOrProperty("jti")
                .hasFieldOrPropertyWithValue("iat", Date.from(now))
                .hasFieldOrPropertyWithValue("exp", Date.from(now.plusSeconds(600)));
    }

    @Test
    void provisionRequestReturnsFailureIfPrivateKeySecretNotFound() {
        when(privateKeyResolver.resolvePrivateKey("pk-test", PrivateKey.class)).thenReturn(null);

        var address = createDataAddressWithPrivateKey();
        var resource = createResourceDefinition(address, UUID.randomUUID().toString(), UUID.randomUUID().toString());

        var future = provisioner.provision(resource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StatusResult::failed)
                .extracting(StatusResult::getFailure)
                .satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(FATAL_ERROR);
                    assertThat(failure.getFailureDetail()).contains("pk-test");
                });

        verifyNoInteractions(client);
    }

    @Test
    void provisionReturnFailureIfServerCallFails() {
        when(client.requestToken(any())).thenReturn(Result.failure("error test"));

        var address = createDataAddressWithSharedSecret();
        var resource = createResourceDefinition(address, UUID.randomUUID().toString(), UUID.randomUUID().toString());

        var future = provisioner.provision(resource, simplePolicy());

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
                .dataAddress(createDataAddressWithSharedSecret())
                .hasToken(true)
                .build();

        var future = provisioner.deprovision(provisionedResource, simplePolicy());

        assertThat(future).succeedsWithin(10, SECONDS).matches(AbstractResult::succeeded)
                .extracting(AbstractResult::getContent).asInstanceOf(type(DeprovisionedResource.class))
                .satisfies(deprovisioned -> {
                    assertThat(deprovisioned.getProvisionedResourceId()).isEqualTo(provisionedResourceId);
                });
    }

    private Oauth2ResourceDefinition createResourceDefinition(DataAddress address, String resourceDefinitionId, String transferProcessId) {
        return Oauth2ResourceDefinition.Builder.newInstance()
                .id(resourceDefinitionId)
                .transferProcessId(transferProcessId)
                .dataAddress(address)
                .build();
    }

    private DataAddress createDataAddressWithSharedSecret() {
        return defaultAddress()
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "clientSecret")
                .build();
    }

    private DataAddress createDataAddressWithPrivateKey() {
        return defaultAddress()
                .property(Oauth2DataAddressSchema.PRIVATE_KEY_NAME, "pk-test")
                .property(Oauth2DataAddressSchema.VALIDITY, "600")
                .build();
    }

    private DataAddress.Builder defaultAddress() {
        return HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "clientId")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "http://oauth2-server.com:");
    }

    private RSAKey generateKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    private Policy simplePolicy() {
        return Policy.Builder.newInstance().build();
    }
}
