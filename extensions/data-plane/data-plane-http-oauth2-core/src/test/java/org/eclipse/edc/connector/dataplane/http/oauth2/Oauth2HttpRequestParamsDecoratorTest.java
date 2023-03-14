/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.http.oauth2;

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_SECRET_KEY;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.TOKEN_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Oauth2HttpRequestParamsDecoratorTest {

    private final Oauth2CredentialsRequestFactory requestFactory = mock(Oauth2CredentialsRequestFactory.class);
    private final Oauth2Client client = mock(Oauth2Client.class);

    private final Oauth2HttpRequestParamsDecorator decorator = new Oauth2HttpRequestParamsDecorator(requestFactory, client);

    @Test
    void requestOauth2TokenAndSetItOnRequest() {
        var dataFlowRequest = dummyDataFlowRequest();
        var httpAddress = httpDataAddressWithOauth2Properties();
        when(requestFactory.create(any())).thenReturn(Result.success(createRequest()));
        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token-test").build()));
        var paramsBuilder = HttpRequestParams.Builder.newInstance().baseUrl("http://any").method("GET");

        var result = decorator.decorate(dataFlowRequest, httpAddress, paramsBuilder).build();

        assertThat(result.getHeaders()).asInstanceOf(map(String.class, String.class))
                .containsEntry("Authorization", "Bearer token-test");
    }

    @Test
    void shouldThrowExceptionIfCannotBuildRequest() {
        var dataFlowRequest = dummyDataFlowRequest();
        var httpAddress = httpDataAddressWithOauth2Properties();
        when(requestFactory.create(any())).thenReturn(Result.failure("cannot build request"));
        var paramsBuilder = HttpRequestParams.Builder.newInstance().baseUrl("http://any").method("GET");

        assertThatThrownBy(() -> decorator.decorate(dataFlowRequest, httpAddress, paramsBuilder))
                .isInstanceOf(EdcException.class);
    }

    @Test
    void shouldThrowExceptionIfCannotGetToken() {
        var dataFlowRequest = dummyDataFlowRequest();
        var httpAddress = httpDataAddressWithOauth2Properties();
        when(requestFactory.create(any())).thenReturn(Result.success(createRequest()));
        when(client.requestToken(any())).thenReturn(Result.failure("Cannot get token"));
        var paramsBuilder = HttpRequestParams.Builder.newInstance().baseUrl("http://any").method("GET");

        assertThatThrownBy(() -> decorator.decorate(dataFlowRequest, httpAddress, paramsBuilder))
                .isInstanceOf(EdcException.class);
    }

    @Test
    void shouldDoNothingIfNoOauthPropertiesContained() {
        var dataFlowRequest = dummyDataFlowRequest();
        var httpAddress = HttpDataAddress.Builder.newInstance().build();
        var paramsBuilder = HttpRequestParams.Builder.newInstance().baseUrl("http://any").method("GET");

        var result = decorator.decorate(dataFlowRequest, httpAddress, paramsBuilder).build();

        assertThat(result.getHeaders()).asInstanceOf(map(String.class, String.class)).isEmpty();
        verifyNoInteractions(requestFactory, client);
    }

    private HttpDataAddress httpDataAddressWithOauth2Properties() {
        return HttpDataAddress.Builder.newInstance()
                .property(TOKEN_URL, "any")
                .property(CLIENT_ID, "any")
                .property(CLIENT_SECRET_KEY, "any")
                .build();
    }

    private DataFlowRequest dummyDataFlowRequest() {
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(dummyAddress())
                .destinationDataAddress(dummyAddress())
                .properties(emptyMap())
                .build();
    }

    private SharedSecretOauth2CredentialsRequest createRequest() {
        return SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url("http://any")
                .grantType("any")
                .clientId("any").clientSecret("any").build();
    }

    private HttpDataAddress dummyAddress() {
        return HttpDataAddress.Builder.newInstance().baseUrl("http://dummy").build();
    }
}
