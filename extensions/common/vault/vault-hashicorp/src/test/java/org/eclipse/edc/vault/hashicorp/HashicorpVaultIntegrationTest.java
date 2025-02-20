/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *       Bayerische Motoren Werke Aktiengesellschaft - Refactoring
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProviderImpl;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings.VAULT_API_HEALTH_PATH_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings.VAULT_API_SECRET_PATH_DEFAULT;
import static org.mockito.Mockito.mock;

@ComponentTest
@Testcontainers
class HashicorpVaultIntegrationTest {
    static final String DOCKER_IMAGE_NAME = "hashicorp/vault:1.18.3";
    static final String VAULT_ENTRY_KEY = "testing";
    static final String VAULT_ENTRY_VALUE = UUID.randomUUID().toString();
    static final String VAULT_DATA_ENTRY_NAME = "content";
    static final String TOKEN = UUID.randomUUID().toString();
    @Container
    private static final VaultContainer<?> VAULTCONTAINER = new VaultContainer<>(DOCKER_IMAGE_NAME)
            .withVaultToken(TOKEN)
            .withSecretInVault("secret/" + VAULT_ENTRY_KEY, format("%s=%s", VAULT_DATA_ENTRY_NAME, VAULT_ENTRY_VALUE));
    public final Monitor monitor = mock();
    private final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private HashicorpVault vault;


    @BeforeEach
    void setUp() {
        var settings = HashicorpVaultSettings.Builder.newInstance()
                .url("http://localhost:%d".formatted(getPort()))
                .ttl(100)
                .healthCheckPath(VAULT_API_HEALTH_PATH_DEFAULT)
                .secretPath(VAULT_API_SECRET_PATH_DEFAULT)
                .build();
        var httpClient = new EdcHttpClientImpl(new OkHttpClient.Builder().build(), RetryPolicy.ofDefaults(), monitor);
        var tokenProvider = new HashicorpVaultTokenProviderImpl(TOKEN);
        vault = new HashicorpVault(monitor, settings, httpClient, objectMapper, tokenProvider);
    }

    @Test
    @DisplayName("Resolve a secret that exists")
    void testResolveSecret_exists() {
        var secretValue = vault.resolveSecret(VAULT_ENTRY_KEY);
        assertThat(secretValue).isEqualTo(VAULT_ENTRY_VALUE);
    }

    @Test
    @DisplayName("Resolve a secret from a sub directory")
    void testResolveSecret_inSubDirectory() {
        var key = "sub/" + VAULT_ENTRY_KEY;
        var value = key + "value";

        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo!bar", "foo.bar", "foo[bar]", "sub/foo{bar}"})
    @DisplayName("Resolve a secret with url encoded characters")
    void testResolveSecret_withUrlEncodedCharacters(String key) {
        var value = key + "value";
        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value);
    }

    @Test
    @DisplayName("Resolve a secret that does not exist")
    void testResolveSecret_doesNotExist() {
        assertThat(vault.resolveSecret("wrong_key")).isNull();
    }

    @Test
    @DisplayName("Update a secret that exists")
    void testSetSecret_exists() {
        var key = UUID.randomUUID().toString();
        var value1 = UUID.randomUUID().toString();
        var value2 = UUID.randomUUID().toString();

        vault.storeSecret(key, value1);
        vault.storeSecret(key, value2);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value2);
    }

    @Test
    @DisplayName("Create a secret that does not exist")
    void testSetSecret_doesNotExist() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value);
    }

    @Test
    @DisplayName("Delete a secret that exists")
    void testDeleteSecret_exists() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        vault.storeSecret(key, value);
        assertThat(vault.deleteSecret(key)).isSucceeded();


        assertThat(vault.resolveSecret(key)).isNull();
    }

    @Test
    @DisplayName("Try to delete a secret that does not exist")
    void testDeleteSecret_doesNotExist() {
        var key = UUID.randomUUID().toString();

        assertThat(vault.deleteSecret(key)).isSucceeded();
        assertThat(vault.resolveSecret(key)).isNull();
    }

    private Integer getPort() {
        // container might not be started, lazily start and wait for it to come up
        if (!VAULTCONTAINER.isRunning()) {
            VAULTCONTAINER.start();
            VAULTCONTAINER.waitingFor(Wait.forHealthcheck());
        }
        return VAULTCONTAINER.getFirstMappedPort();
    }

}
