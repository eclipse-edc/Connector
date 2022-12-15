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

class PrivateKeyOauth2CredentialsRequestTest {

    @Test
    void verifyClientAssertionMandatory() {
        assertThatNullPointerException().isThrownBy(() -> defaultRequest().build())
                .withMessageContaining("client_assertion");
    }

    @Test
    void verifyParams() {
        var request = defaultRequest()
                .clientAssertion("assertion")
                .param("foo", "bar")
                .build();

        assertThat(request.getParams()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "foo", "bar",
                "grant_type", "grantType",
                "client_assertion", "assertion",
                "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
        ));
    }

    private PrivateKeyOauth2CredentialsRequest.Builder defaultRequest() {
        return PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                .url("http://example.com")
                .grantType("grantType");
    }
}