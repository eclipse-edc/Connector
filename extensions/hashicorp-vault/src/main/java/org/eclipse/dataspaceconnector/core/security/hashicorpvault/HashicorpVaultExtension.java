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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.Duration;

// Is the VaultExtension interface just gone?
@Provides({ Vault.class, PrivateKeyResolver.class, CertificateResolver.class })
public class HashicorpVaultExtension implements ServiceExtension {

  @EdcSetting(required = true)
  public static final String VAULT_URL = "edc.vault.hashicorp.url";

  @EdcSetting(required = true)
  public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";

  @EdcSetting
  private static final String VAULT_TIMEOUT_SECONDS = "edc.vault.hashicorp.timeout.seconds";

  private Vault vault;
  private CertificateResolver certificateResolver;
  private PrivateKeyResolver privateKeyResolver;

  @Override
  public String name() {
    return "Hashicorp Vault";
  }

  public Vault getVault() {
    return vault;
  }

  public PrivateKeyResolver getPrivateKeyResolver() {
    return privateKeyResolver;
  }

  public CertificateResolver getCertificateResolver() {
    return certificateResolver;
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    HashicorpVaultClientConfig config = loadHashicorpVaultClientConfig(context);

    OkHttpClient okHttpClient = createOkHttpClient(config);
    HashicorpVaultClient client =
        new HashicorpVaultClient(config, okHttpClient, context.getTypeManager().getMapper());

    vault = new HashicorpVault(client, context.getMonitor());
    certificateResolver = new HashicorpCertificateResolver(vault, context.getMonitor());
    privateKeyResolver = new VaultPrivateKeyResolver(vault);

    context.registerService(Vault.class, vault);
    context.registerService(CertificateResolver.class, certificateResolver);
    context.registerService(PrivateKeyResolver.class, privateKeyResolver);

    context.getMonitor().info("HashicorpVaultExtension: authentication/initialization complete.");
  }

  private OkHttpClient createOkHttpClient(HashicorpVaultClientConfig config) {
    OkHttpClient.Builder builder =
        new OkHttpClient.Builder()
            .callTimeout(config.getTimeout())
            .readTimeout(config.getTimeout());

    return builder.build();
  }

  private HashicorpVaultClientConfig loadHashicorpVaultClientConfig(
      ServiceExtensionContext context) {

    String vaultUrl = context.getSetting(VAULT_URL, null);
    if (vaultUrl == null) {
      throw new HashicorpVaultException(String.format("Vault URL (%s) must be defined", VAULT_URL));
    }

    int vaultTimeoutSeconds = Math.max(0, context.getSetting(VAULT_TIMEOUT_SECONDS, 30));
    Duration vaultTimeoutDuration = Duration.ofSeconds(vaultTimeoutSeconds);

    String vaultToken = context.getSetting(VAULT_TOKEN, null);

    if (vaultToken == null) {
      throw new EdcException(
          String.format("For Vault authentication [%s] is required", VAULT_TOKEN));
    }

    return HashicorpVaultClientConfig.builder()
        .vaultUrl(vaultUrl)
        .vaultToken(vaultToken)
        .timeout(vaultTimeoutDuration)
        .build();
  }
}
