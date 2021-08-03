/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.security.fs;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.VaultExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.eclipse.edc.security.fs.FsConfiguration.*;

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
