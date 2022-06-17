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

import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class HashicorpVaultIT extends AbstractHashicorpIT {

  @Test
  @DisplayName("Resolve a secret that exists")
  void testResolveSecret_exists() {
    Vault vault = getVault();
    String secretValue = vault.resolveSecret(VAULT_ENTRY_KEY);
    Assertions.assertEquals(VAULT_ENTRY_VALUE, secretValue);
  }

  @Test
  @DisplayName("Resolve a secret that does not exist")
  void testResolveSecret_doesNotExist() {
    Vault vault = getVault();
    Assertions.assertNull(vault.resolveSecret("wrong_key"));
  }

  @Test
  @DisplayName("Update a secret that exists")
  void testSetSecret_exists() {
    String key = UUID.randomUUID().toString();
    String value1 = UUID.randomUUID().toString();
    String value2 = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.storeSecret(key, value1);
    vault.storeSecret(key, value2);
    String secretValue = vault.resolveSecret(key);
    Assertions.assertEquals(value2, secretValue);
  }

  @Test
  @DisplayName("Create a secret that does not exist")
  void testSetSecret_doesNotExist() {
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.storeSecret(key, value);
    String secretValue = vault.resolveSecret(key);
    Assertions.assertEquals(value, secretValue);
  }

  @Test
  @DisplayName("Delete a secret that exists")
  void testDeleteSecret_exists() {
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.storeSecret(key, value);
    vault.deleteSecret(key);

    Assertions.assertNull(vault.resolveSecret(key));
  }

  @Test
  @DisplayName("Try to delete a secret that does not exist")
  void testDeleteSecret_doesNotExist() {
    String key = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.deleteSecret(key);

    Assertions.assertNull(vault.resolveSecret(key));
  }
}
