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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings.VAULT_TOKEN_RENEW_BUFFER_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings.VAULT_TOKEN_TTL_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HashicorpVaultSettingsTest {

    private static final String TOKEN = "token";
    private static final String URL = "https://test.com/vault";
    private static final String HEALTH_CHECK_PATH = "/healthcheck/path";
    private static final String SECRET_PATH = "/secret/path";

    @Test
    void createSettings_withDefaultValues_shouldSucceed() {
        var settings = assertDoesNotThrow(() -> createSettings(
                URL,
                HEALTH_CHECK_PATH, VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(settings.url()).isEqualTo(URL);
        assertThat(settings.healthCheckEnabled()).isEqualTo(true);
        assertThat(settings.healthCheckPath()).isEqualTo(HEALTH_CHECK_PATH);
        assertThat(settings.healthStandbyOk()).isEqualTo(true);
        assertThat(settings.ttl()).isEqualTo(VAULT_TOKEN_TTL_DEFAULT);
        assertThat(settings.renewBuffer()).isEqualTo(VAULT_TOKEN_RENEW_BUFFER_DEFAULT);
        assertThat(settings.secretPath()).isEqualTo(SECRET_PATH);
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

    private HashicorpVaultSettings createSettings(String url,
                                                  String healthCheckPath,
                                                  long ttl,
                                                  long renewBuffer) {
        return HashicorpVaultSettings.Builder.newInstance()
                .url(url)
                .healthCheckEnabled(true)
                .healthCheckPath(healthCheckPath)
                .healthStandbyOk(true)
                .ttl(ttl)
                .renewBuffer(renewBuffer)
                .secretPath(SECRET_PATH)
                .build();
    }
}
