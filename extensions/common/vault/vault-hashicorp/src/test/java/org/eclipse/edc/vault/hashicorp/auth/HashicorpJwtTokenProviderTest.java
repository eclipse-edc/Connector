/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class HashicorpJwtTokenProviderTest {
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    private HashicorpJwtTokenProvider tokenProvider;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();


    @BeforeEach
    void setup() {
        wireMock.stubFor();

        tokenProvider = HashicorpJwtTokenProvider.Builder.newInstance()
                .role("test-role")
                .tokenUrl(wireMock.baseUrl())
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .build();


    }
}