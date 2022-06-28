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

import org.eclipse.dataspaceconnector.common.util.junit.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@IntegrationTest
@Tag("HashicorpVaultIntegrationTest")
class HashicorpVaultIT extends AbstractHashicorpIT {

    @Test
    @DisplayName("Resolve a secret that exists")
    void testResolveSecret_exists() {
        var vault = testExtension.getVault();
        var key = UUID.randomUUID().toString();
        var valueExpected = UUID.randomUUID().toString();

        vault.storeSecret(key, valueExpected);
        var secretValue = vault.resolveSecret(key);
        Assertions.assertEquals(valueExpected, secretValue);
    }

    @Test
    @DisplayName("Resolve a secret that does not exist")
    void testResolveSecret_doesNotExist() {
        var vault = testExtension.getVault();
        Assertions.assertNull(vault.resolveSecret("wrong_key"));
    }

    @Test
    @DisplayName("Update a secret that exists")
    void testSetSecret_exists() {
        var key = UUID.randomUUID().toString();
        var value1 = UUID.randomUUID().toString();
        var value2 = UUID.randomUUID().toString();

        var vault = testExtension.getVault();
        vault.storeSecret(key, value1);
        vault.storeSecret(key, value2);
        var secretValue = vault.resolveSecret(key);
        Assertions.assertEquals(value2, secretValue);
    }

    @Test
    @DisplayName("Create a secret that does not exist")
    void testSetSecret_doesNotExist() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        var vault = testExtension.getVault();
        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        Assertions.assertEquals(value, secretValue);
    }

    @Test
    @DisplayName("Delete a secret that exists")
    void testDeleteSecret_exists() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        Vault vault = testExtension.getVault();
        vault.storeSecret(key, value);
        vault.deleteSecret(key);

        Assertions.assertNull(vault.resolveSecret(key));
    }

    @Test
    @DisplayName("Delete a secret that does not exist")
    void testDeleteSecret_doesNotExist() {
        var key = UUID.randomUUID().toString();

        var vault = testExtension.getVault();
        vault.deleteSecret(key);

        Assertions.assertNull(vault.resolveSecret(key));
    }
}
