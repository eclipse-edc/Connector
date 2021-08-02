/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements a vault backed by a properties file.
 */
public class FsVault implements Vault {
    private AtomicReference<Map<String, String>> secrets;
    private Path vaultFile;
    private boolean persistent;

    public FsVault(Path vaultFile, boolean persistent) {
        this.persistent = persistent;
        secrets = new AtomicReference<>(new HashMap<>());

        this.vaultFile = vaultFile;
        try (final InputStream is = Files.newInputStream(vaultFile)) {
            var properties = new Properties();
            properties.load(is);
            for (String name : properties.stringPropertyNames()) {
                secrets.get().put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return secrets.get().get(key);
    }

    @Override
    public synchronized VaultResponse storeSecret(String key, String value) {
        var newSecrets = new HashMap<>(secrets.get());
        newSecrets.put(key, value);
        var properties = new Properties();
        properties.putAll(newSecrets);
        if (persistent) {
            try (Writer writer = Files.newBufferedWriter(vaultFile)) {
                properties.store(writer, null);
            } catch (IOException e) {
                return new VaultResponse(e.getMessage());
            }
        }
        secrets.set(newSecrets);
        return VaultResponse.OK;
    }

    @Override
    public VaultResponse deleteSecret(String key) {
        var newSecrets = new HashMap<>(secrets.get());
        newSecrets.remove(key);
        var properties = new Properties();
        properties.putAll(newSecrets);
        if (persistent) {
            try (Writer writer = Files.newBufferedWriter(vaultFile)) {
                properties.store(writer, null);
            } catch (IOException e) {
                return new VaultResponse(e.getMessage());
            }
        }
        secrets.set(newSecrets);
        return VaultResponse.OK;
    }
}
