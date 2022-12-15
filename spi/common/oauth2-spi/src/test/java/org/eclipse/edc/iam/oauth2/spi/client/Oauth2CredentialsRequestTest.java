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

class Oauth2CredentialsRequestTest {

    @Test
    void verifyUrlMandatory() {
        assertThatNullPointerException().isThrownBy(() -> TestRequest.Builder.newInstance().build())
                .withMessageContaining("url");
    }

    @Test
    void verifyGrandTypeMandatory() {
        assertThatNullPointerException().isThrownBy(() -> TestRequest.Builder.newInstance().url("http://example.com").build())
                .withMessageContaining("grant_type");
    }

    @Test
    void verifyGetMethods() {
        var request = TestRequest.Builder.newInstance()
                .url("http://example.com")
                .grantType("client_credentials")
                .param("foo", "bar")
                .build();

        assertThat(request.getUrl()).isEqualTo("http://example.com");
        assertThat(request.getGrantType()).isEqualTo("client_credentials");
        assertThat(request.getParams()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "foo", "bar",
                "grant_type", "client_credentials"
        ));
    }
}

