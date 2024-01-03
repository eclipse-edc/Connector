/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.vault.filesystem;

import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.eclipse.edc.vault.filesystem.FsConfiguration.KEYSTORE_LOCATION;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.KEYSTORE_PASSWORD;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.PERSISTENT_VAULT;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.VAULT_LOCATION;

/**
 * Bootstraps the file system-based vault extension.
 */
@BaseExtension
@Provides({ PrivateKeyResolver.class, CertificateResolver.class })
@Extension(value = FsVaultExtension.NAME)
public class FsVaultExtension implements ServiceExtension {

    public static final String NAME = "FS Vault";

    @Inject
    private KeyParserRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var monitor = context.getMonitor();
        monitor.warning("Using the JSK-based Vault and PrivateKeyResolver is intended only for testing and demo purposes. Do NOT use this in a production scenario!");

        var keyStore = loadKeyStore(context);
        var keystorePassword = context.getSetting(KEYSTORE_PASSWORD, null);
        var privateKeyResolver = new JksPrivateKeyResolver(registry, keystorePassword, keyStore, monitor);
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);
        var certificateResolver = new FsCertificateResolver(keyStore);
        context.registerService(CertificateResolver.class, certificateResolver);
    }

    @Provider
    public Vault vault(ServiceExtensionContext context) {
        var vaultLocation = context.getSetting(VAULT_LOCATION, "dataspaceconnector-vault.properties");
        var vaultPath = Paths.get(vaultLocation);
        if (!Files.exists(vaultPath)) {
            throw new EdcException("Vault file does not exist: " + vaultLocation);
        }
        var persistentVault = context.getSetting(PERSISTENT_VAULT, true);
        return new FsVault(vaultPath, persistentVault);
    }

    private KeyStore loadKeyStore(ServiceExtensionContext context) {
        var keyStoreLocation = context.getSetting(KEYSTORE_LOCATION, "dataspaceconnector-keystore.jks");
        var keyStorePath = Paths.get(keyStoreLocation);
        if (!Files.exists(keyStorePath)) {
            throw new EdcException("Key store does not exist: " + keyStoreLocation);
        }

        var keystorePassword = context.getSetting(KEYSTORE_PASSWORD, null);
        if (keystorePassword == null) {
            throw new EdcException("Key store password was not specified");
        }

        try (var stream = Files.newInputStream(keyStorePath)) {
            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, keystorePassword.toCharArray());
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

}
