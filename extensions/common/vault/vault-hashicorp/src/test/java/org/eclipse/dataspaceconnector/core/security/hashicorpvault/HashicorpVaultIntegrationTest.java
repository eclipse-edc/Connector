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
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import org.eclipse.dataspaceconnector.core.security.hashicorpvault.annotations.HashicorpVaultTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_TOKEN;
import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_URL;

@HashicorpVaultTest
@ExtendWith(EdcExtension.class)
class HashicorpVaultIntegrationTest {
    private static final String VAULT_TEST_URL = "http://127.0.0.1:8200";
    private static final String VAULT_TEST_TOKEN = "test-token";

    @BeforeEach
    final void setup(EdcExtension extension) {
        extension.setConfiguration(Map.of(VAULT_URL, VAULT_TEST_URL, VAULT_TOKEN, VAULT_TEST_TOKEN));
    }

    @Test
    @DisplayName("Resolve a secret that exists")
    void resolveSecret_exists(Vault vault) {
        var key = UUID.randomUUID().toString();
        var valueExpected = UUID.randomUUID().toString();

        vault.storeSecret(key, valueExpected);
        var secretValue = vault.resolveSecret(key);

        assertThat(secretValue).isEqualTo(valueExpected);
    }

    @Test
    @DisplayName("Resolve a secret that does not exist")
    void resolveSecret_doesNotExist(Vault vault) {
        assertThat(vault.resolveSecret("wrong_key")).isNull();
    }

    @Test
    @DisplayName("Update a secret that exists")
    void testSetSecret_exists(Vault vault) {
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
    void setSecret_doesNotExist(Vault vault) {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);

        assertThat(secretValue).isEqualTo(value);
    }

    @Test
    @DisplayName("Delete a secret that exists")
    void deleteSecret_exists(Vault vault) {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        vault.storeSecret(key, value);
        vault.deleteSecret(key);

        assertThat(vault.resolveSecret(key)).isNull();
    }

    @Test
    @DisplayName("Delete a secret that does not exist")
    void deleteSecret_doesNotExist(Vault vault) {
        var key = UUID.randomUUID().toString();

        vault.deleteSecret(key);

        assertThat(vault.resolveSecret(key)).isNull();
    }
}
