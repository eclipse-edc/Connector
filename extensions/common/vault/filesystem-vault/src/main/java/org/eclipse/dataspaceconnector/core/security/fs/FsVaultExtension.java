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

package org.eclipse.dataspaceconnector.core.security.fs;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.BaseExtension;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.eclipse.dataspaceconnector.core.security.fs.FsConfiguration.KEYSTORE_LOCATION;
import static org.eclipse.dataspaceconnector.core.security.fs.FsConfiguration.KEYSTORE_PASSWORD;
import static org.eclipse.dataspaceconnector.core.security.fs.FsConfiguration.PERSISTENT_VAULT;
import static org.eclipse.dataspaceconnector.core.security.fs.FsConfiguration.VAULT_LOCATION;

/**
 * Bootstraps the file system-based vault extension.
 */
@BaseExtension
@Provides({ Vault.class, PrivateKeyResolver.class, CertificateResolver.class })
public class FsVaultExtension implements ServiceExtension {

    @Override
    public String name() {
        return "FS Vault";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = initializeVault();
        context.registerService(Vault.class, vault);

        KeyStore keyStore = loadKeyStore();
        var privateKeyResolver = new FsPrivateKeyResolver(KEYSTORE_PASSWORD, keyStore);
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);

        var certificateResolver = new FsCertificateResolver(keyStore);
        context.registerService(CertificateResolver.class, certificateResolver);
    }

    private Vault initializeVault() {
        var vaultPath = Paths.get(VAULT_LOCATION);
        if (!Files.exists(vaultPath)) {
            throw new EdcException("Vault file does not exist: " + VAULT_LOCATION);
        }
        return new FsVault(vaultPath, PERSISTENT_VAULT);
    }

    private KeyStore loadKeyStore() {
        var keyStorePath = Paths.get(KEYSTORE_LOCATION);
        if (!Files.exists(keyStorePath)) {
            throw new EdcException("Key store does not exist: " + KEYSTORE_LOCATION);
        }

        if (KEYSTORE_PASSWORD == null) {
            throw new EdcException("Key store password was not specified");
        }
        
        try (InputStream stream = Files.newInputStream(keyStorePath)) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, KEYSTORE_PASSWORD.toCharArray());
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

}
