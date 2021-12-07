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

package org.eclipse.dataspaceconnector.security.fs;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.eclipse.dataspaceconnector.security.fs.FsConfiguration.KEYSTORE_LOCATION;
import static org.eclipse.dataspaceconnector.security.fs.FsConfiguration.KEYSTORE_PASSWORD;
import static org.eclipse.dataspaceconnector.security.fs.FsConfiguration.PERSISTENT_VAULT;
import static org.eclipse.dataspaceconnector.security.fs.FsConfiguration.VAULT_LOCATION;

/**
 * Bootstraps the file system-based vault extension.
 */
public class FsVaultExtension implements VaultExtension {
    private Vault vault;
    private PrivateKeyResolver privateKeyResolver;
    private CertificateResolver certificateResolver;

    @Override
    public void initialize(Monitor monitor) {
        vault = initializeVault();

        KeyStore keyStore = loadKeyStore();
        privateKeyResolver = new FsPrivateKeyResolver(KEYSTORE_PASSWORD, keyStore);
        certificateResolver = new FsCertificateResolver(keyStore);

        monitor.info("Initialized FS Vault extension");
    }

    @Override
    public Vault getVault() {
        return vault;
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return privateKeyResolver;
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return certificateResolver;
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

        try (InputStream stream = Files.newInputStream(keyStorePath)) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, KEYSTORE_PASSWORD.toCharArray());
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

}
