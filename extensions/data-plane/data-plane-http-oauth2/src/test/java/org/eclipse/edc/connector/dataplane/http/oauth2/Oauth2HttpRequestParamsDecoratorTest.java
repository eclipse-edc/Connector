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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Oauth2HttpRequestParamsDecoratorTest {

    private final Oauth2CredentialsRequestFactory requestFactory = mock(Oauth2CredentialsRequestFactory.class);
    private final Oauth2Client client = mock(Oauth2Client.class);

    @Test
    void requestOauth2TokenAndSetItOnRequest() {
        var decorator = new Oauth2HttpRequestParamsDecorator(requestFactory, client);
        var dataFlowRequest = dummyDataFlowRequest();
        var httpAddress = HttpDataAddress.Builder.newInstance().build();
        when(requestFactory.create(any())).thenReturn(Result.success(createRequest()));
        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token-test").build()));
        var paramsBuilder = HttpRequestParams.Builder.newInstance().baseUrl("http://any").method("GET");

        var result = decorator.decorate(dataFlowRequest, httpAddress, paramsBuilder).build();

        assertThat(result.getHeaders()).asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
                .containsEntry("Authorization", "Bearer token-test");
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
