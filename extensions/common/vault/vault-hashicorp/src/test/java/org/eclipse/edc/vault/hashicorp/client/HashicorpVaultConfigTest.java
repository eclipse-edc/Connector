/*
 *  Copyright (c) 2024 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig.VAULT_TOKEN_TTL_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class HashicorpVaultConfigTest {

    private static final String URL = "https://test.com/vault";
    private static final String HEALTH_CHECK_PATH = "/healthcheck/path";
    private static final String SECRET_PATH = "/secret/path";

    @Test
    void createSettings_withDefaultValues_shouldSucceed() {
        var settings = assertDoesNotThrow(() -> createSettings(
                URL,
                HEALTH_CHECK_PATH, VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(settings.getVaultUrl()).isEqualTo(URL);
        assertThat(settings.getHealthCheckEnabled()).isEqualTo(true);
        assertThat(settings.getHealthCheckPath()).isEqualTo("healthcheck/path");
        assertThat(settings.getHealthStandbyOk()).isEqualTo(true);
        assertThat(settings.getTtl()).isEqualTo(VAULT_TOKEN_TTL_DEFAULT);
        assertThat(settings.getRenewBuffer()).isEqualTo(VAULT_TOKEN_RENEW_BUFFER_DEFAULT);
        assertThat(settings.getSecretPath()).isEqualTo("secret/path");
        assertThat(settings.getFolderPath()).isNull();
    }

    @Test
    void createSettings_withVaultUrlNull_shouldThrowException() {
        var throwable = assertThrows(Exception.class, () -> createSettings(
                null,
                HEALTH_CHECK_PATH, VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault url must not be null");
    }

    @Test
    void createSettings_withHealthCheckPathNull_shouldThrowException() {
        var throwable = assertThrows(Exception.class, () -> createSettings(
                URL,
                null,
                VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault health check path must not be null");
    }

    @Test
    void createSettings_withVaultTokenTtlLessThan5_shouldThrowException() {
        var throwable = assertThrows(Exception.class, () -> createSettings(
                URL,
                HEALTH_CHECK_PATH,
                4,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault token ttl minimum value is 5");
    }

    @ParameterizedTest
    @ValueSource(longs = { VAULT_TOKEN_TTL_DEFAULT, VAULT_TOKEN_TTL_DEFAULT + 1 })
    void createSettings_withVaultTokenRenewBufferEqualOrGreaterThanTtl_shouldThrowException(long value) {
        var throwable = assertThrows(Exception.class, () -> createSettings(
                URL,
                HEALTH_CHECK_PATH,
                VAULT_TOKEN_TTL_DEFAULT,
                value));
        assertThat(throwable.getMessage()).isEqualTo("Vault token renew buffer value must be less than ttl value");
    }

    @ParameterizedTest
    @ArgumentsSource(PathsToTrimProvider.class)
    void getHealthCheckPath_shouldTrimLeadingAndTrailingSlash(String raw, String expected) {
        var settings = builderWithDefaults().healthCheckPath(raw).build();

        assertThat(settings.getHealthCheckPath()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ArgumentsSource(PathsToTrimProvider.class)
    void getSecretPath_shouldTrimLeadingAndTrailingSlash(String raw, String expected) {
        var settings = builderWithDefaults().secretPath(raw).build();

        assertThat(settings.getSecretPath()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ArgumentsSource(PathsToTrimProvider.class)
    void getFolderPath_shouldTrimLeadingAndTrailingSlash(String raw, String expected) {
        var settings = builderWithDefaults().folderPath(raw).build();

        assertThat(settings.getFolderPath()).isEqualTo(expected);
    }

    private HashicorpVaultConfig.Builder builderWithDefaults() {
        return HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(URL)
                .ttl(VAULT_TOKEN_TTL_DEFAULT);
    }

    private HashicorpVaultConfig createSettings(String url,
                                                String healthCheckPath,
                                                long ttl,
                                                long renewBuffer) {
        return HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(url)
                .healthCheckEnabled(true)
                .healthCheckPath(healthCheckPath)
                .healthStandbyOk(true)
                .ttl(ttl)
                .renewBuffer(renewBuffer)
                .secretPath(SECRET_PATH)
                .build();
    }

    private static class PathsToTrimProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    arguments("/path", "path"),                 // leading slash
                    arguments("path/", "path"),                 // trailing slash
                    arguments("/path/", "path"),                // both
                    arguments("/nested/path/", "nested/path"),  // nested, inner slashes kept
                    arguments("path", "path")                   // already trimmed
            );
        }
    }
}
