/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.spi.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SharedSecretOauth2CredentialsRequestTest {

    @Test
    void verifyClientIdMandatory() {
        assertThatNullPointerException().isThrownBy(() -> defaultRequest().build())
                .withMessageContaining("client_id");
    }

    @Test
    void verifyClientSecretTypeMandatory() {
        assertThatNullPointerException().isThrownBy(() -> defaultRequest().clientId("clientId").build())
                .withMessageContaining("client_secret");
    }

    @Test
    void verifyParams() {
        var request = defaultRequest()
                .clientId("clientId")
                .clientSecret("clientSecret")
                .param("foo", "bar")
                .build();

        assertThat(request.getParams()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "foo", "bar",
                "grant_type", "grantType",
                "client_id", "clientId",
                "client_secret", "clientSecret"
        ));
    }

    private SharedSecretOauth2CredentialsRequest.Builder defaultRequest() {
        return SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url("http://example.com")
                .grantType("grantType");
    }
}