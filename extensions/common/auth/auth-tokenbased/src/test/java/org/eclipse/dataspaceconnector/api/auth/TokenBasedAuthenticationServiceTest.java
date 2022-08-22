/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenBasedAuthenticationServiceTest {

    private static final String TEST_API_KEY = "test-key";
    private TokenBasedAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new TokenBasedAuthenticationService(TEST_API_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "x-api-key", "X-API-KEY", "X-Api-Key" })
    void isAuthorized(String validKey) {
        var map = Map.of(validKey, List.of(TEST_API_KEY));
        assertThat(service.isAuthenticated(map)).isTrue();
    }

    @Test
    void isAuthorized_headerNotPresent_throwsException() {
        var map = Map.of("header1", List.of("val1, val2"),
                "header2", List.of("anotherval1", "anotherval2"));
        assertThatThrownBy(() -> service.isAuthenticated(map)).isInstanceOf(AuthenticationFailedException.class).hasMessage("x-api-key not found");
    }

    @Test
    void isAuthorized_headersEmpty_throwsException() {
        Map<String, List<String>> map = Collections.emptyMap();
        assertThatThrownBy(() -> service.isAuthenticated(map)).isInstanceOf(AuthenticationFailedException.class).hasMessage("x-api-key not found");
    }

    @Test
    void isAuthorized_headersNull_throwsException() {
        assertThatThrownBy(() -> service.isAuthenticated(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isAuthorized_notAuthorized() {
        var map = Map.of("x-api-key", List.of("invalid_api_key"));
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_multipleValues_oneAuthorized() {
        var map = Map.of("x-api-key", List.of("invalid_api_key", TEST_API_KEY));
        assertThat(service.isAuthenticated(map)).isTrue();
    }
}