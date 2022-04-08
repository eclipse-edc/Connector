/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicAuthenticationServiceTest {

    private static final List<BasicAuthenticationExtension.ConfigCredentials> TEST_CREDENTIALS = List.of(
            new BasicAuthenticationExtension.ConfigCredentials("usera", "api-basic-auth-usera"),
            new BasicAuthenticationExtension.ConfigCredentials("userb", "api-basic-auth-userb")
    );
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();

    private final List<String> testCredentialsEncoded = List.of(
            generateBearerToken(format("%s:%s", "usera", "password1")),
            generateBearerToken(format("%s:%s", "userb", "password2"))
    );

    private BasicAuthenticationService service;
    private Vault vault;

    @BeforeEach
    void setUp() {
        vault = mock(Vault.class);
        when(vault.resolveSecret("api-basic-auth-usera")).thenReturn("password1");
        when(vault.resolveSecret("api-basic-auth-userb")).thenReturn("password2");

        service = new BasicAuthenticationService(vault, TEST_CREDENTIALS, mock(Monitor.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Authorization", "authorization", "authoRization" })
    void isAuthorized(String validKey) {
        var map = Map.of(validKey, List.of(testCredentialsEncoded.get(0)));
        var map2 = Map.of(validKey, List.of(testCredentialsEncoded.get(1)));
        assertThat(service.isAuthenticated(map)).isTrue();
        assertThat(service.isAuthenticated(map2)).isTrue();
    }

    @Test
    void isAuthorized_headerNotPresent() {
        var map = Map.of("header1", List.of("val1, val2"),
                "header2", List.of("anotherval1", "anotherval2"));
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_headersEmpty() {
        Map<String, List<String>> map = Collections.emptyMap();
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_headersNull() {
        assertThatThrownBy(() -> service.isAuthenticated(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isAuthorized_notAuthorized() {
        var map = Map.of(
                "authorization", List.of(generateBearerToken("invalid-user:random"))
        );
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_decoderError_notBearerToken() {
        var map = Map.of("authorization", List.of("invalid_auth_header_value"));
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_decoderError_notValidBearerToken() {
        var map = Map.of("authorization", List.of("Bearer invalid_token"));
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_decoderError_missingColon() {
        var map = Map.of("authorization", List.of(generateBearerToken("missingcolon")));
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_multipleValues_firstAuthorized() {
        var map = Map.of("authorization", List.of(
                testCredentialsEncoded.get(0),
                generateBearerToken("invalid-user:random")
        ));
        assertThat(service.isAuthenticated(map)).isTrue();
    }

    @Test
    void isAuthorized_multipleValues_secondAuthorized() {
        var map = Map.of("authorization", List.of(
                generateBearerToken("invalid-user:random"),
                testCredentialsEncoded.get(0)
        ));
        assertThat(service.isAuthenticated(map)).isTrue();
    }

    @Test
    void isAuthorized_multipleValues_notAuthorized() {
        var map = Map.of(
                "authorization", List.of(
                        generateBearerToken("invalid-user:random"),
                        generateBearerToken("invalid-user2:random2")
                )
        );
        assertThat(service.isAuthenticated(map)).isFalse();
    }

    @Test
    void isAuthorized_wrongVaultKey() {
        var wrongVaultKeyConfig = List.of(
                new BasicAuthenticationExtension.ConfigCredentials("usera", "wrong.key")
        );
        var service = new BasicAuthenticationService(vault, wrongVaultKeyConfig, mock(Monitor.class));
        var map = Map.of("authorization", List.of(testCredentialsEncoded.get(0)));

        assertThat(service.isAuthenticated(map)).isFalse();
    }

    private String generateBearerToken(String plainCredentials) {
        return format("Bearer %s", BASE_64_ENCODER.encodeToString(plainCredentials.getBytes()));
    }
}
