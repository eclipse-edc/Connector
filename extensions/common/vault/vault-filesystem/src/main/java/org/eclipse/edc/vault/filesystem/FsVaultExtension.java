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
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.KEYSTORE_LOCATION;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.KEYSTORE_PASSWORD;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.PERSISTENT_VAULT;
import static org.eclipse.edc.vault.filesystem.FsConfiguration.VAULT_LOCATION;

/**
 * Bootstraps the file system-based vault extension.
 */
@BaseExtension
@Provides({ Vault.class, PrivateKeyResolver.class, CertificateResolver.class })
@Extension(value = FsVaultExtension.NAME)
public class FsVaultExtension implements ServiceExtension {

    public static final String NAME = "FS Vault";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = initializeVault();
        context.registerService(Vault.class, vault);

        KeyStore keyStore = loadKeyStore();
        var keystorePassword = propOrEnv(KEYSTORE_PASSWORD, null);
        var privateKeyResolver = new FsPrivateKeyResolver(keystorePassword, keyStore);
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);

        var certificateResolver = new FsCertificateResolver(keyStore);
        context.registerService(CertificateResolver.class, certificateResolver);
    }

    private Vault initializeVault() {
        var vaultLocation = propOrEnv(VAULT_LOCATION, "dataspaceconnector-vault.properties");
        var vaultPath = Paths.get(vaultLocation);
        if (!Files.exists(vaultPath)) {
            throw new EdcException("Vault file does not exist: " + vaultLocation);
        }
        var persistentVault = Boolean.parseBoolean(propOrEnv(PERSISTENT_VAULT, "true"));
        return new FsVault(vaultPath, persistentVault);
    }

    private KeyStore loadKeyStore() {
        var keyStoreLocation = propOrEnv(KEYSTORE_LOCATION, "dataspaceconnector-keystore.jks");
        var keyStorePath = Paths.get(keyStoreLocation);
        if (!Files.exists(keyStorePath)) {
            throw new EdcException("Key store does not exist: " + keyStoreLocation);
        }

        var keystorePassword = propOrEnv(KEYSTORE_PASSWORD, null);
        if (keystorePassword == null) {
            throw new EdcException("Key store password was not specified");
        }

        try (InputStream stream = Files.newInputStream(keyStorePath)) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, keystorePassword.toCharArray());
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

}
