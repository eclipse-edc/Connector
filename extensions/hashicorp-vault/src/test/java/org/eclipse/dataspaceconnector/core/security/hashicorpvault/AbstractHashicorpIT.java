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

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import java.util.HashMap;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultClient.VAULT_DATA_ENTRY_NAME;
import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_TOKEN;
import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_URL;

@Testcontainers
@ExtendWith(EdcExtension.class)
class AbstractHashicorpIT {
  static final String DOCKER_IMAGE_NAME = "vault:1.9.6";
  static final String VAULT_ENTRY_KEY = "testing";
  static final String VAULT_ENTRY_VALUE = UUID.randomUUID().toString();
  static final String TOKEN = UUID.randomUUID().toString();

  private final TestExtension testExtension = new TestExtension();

  protected Vault getVault() {
    return testExtension.getVault();
  }

  protected CertificateResolver getCertificateResolver() {
    return testExtension.getCertificateResolver();
  }

  @Container @ClassRule
  private static final VaultContainer<?> vaultContainer =
      new VaultContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
          .withVaultToken(TOKEN)
          .withSecretInVault(
              "secret/" + VAULT_ENTRY_KEY,
              String.format("%s=%s", VAULT_DATA_ENTRY_NAME, VAULT_ENTRY_VALUE));

  @BeforeEach
  final void beforeEach(EdcExtension extension) {
    extension.setConfiguration(
        new HashMap<>() {
          {
            put(
                VAULT_URL,
                String.format(
                    "http://%s:%s", vaultContainer.getHost(), vaultContainer.getFirstMappedPort()));
            put(VAULT_TOKEN, TOKEN);
          }
        });
    extension.registerSystemExtension(ServiceExtension.class, testExtension);
  }

  private static class TestExtension implements ServiceExtension {
    private Vault vault;
    private CertificateResolver certificateResolver;

    @Override
    public void initialize(ServiceExtensionContext context) {
      vault = context.getService(Vault.class);
      certificateResolver = context.getService(CertificateResolver.class);
    }

    public Vault getVault() {
      return this.vault;
    }

    public CertificateResolver getCertificateResolver() {
      return this.certificateResolver;
    }
  }
}
