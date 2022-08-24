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
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
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
    private final AtomicReference<Map<String, String>> secrets;
    private final Path vaultFile;
    private final boolean persistent;

    public FsVault(Path vaultFile, boolean persistent) {
        this.persistent = persistent;
        secrets = new AtomicReference<>(new HashMap<>());

        this.vaultFile = vaultFile;
        try (InputStream is = Files.newInputStream(vaultFile)) {
            var properties = new Properties();
            properties.load(is);
            for (String name : properties.stringPropertyNames()) {
                secrets.get().put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public @Nullable
    String resolveSecret(String key) {
        return secrets.get().get(key);
    }

    @Override
    public synchronized Result<Void> storeSecret(String key, String value) {
        var newSecrets = new HashMap<>(secrets.get());
        newSecrets.put(key, value);
        var properties = new Properties();
        properties.putAll(newSecrets);
        if (persistent) {
            try (Writer writer = Files.newBufferedWriter(vaultFile)) {
                properties.store(writer, null);
            } catch (IOException e) {
                return Result.failure(e.getMessage());
            }
        }
        secrets.set(newSecrets);
        return Result.success();
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        var newSecrets = new HashMap<>(secrets.get());
        newSecrets.remove(key);
        var properties = new Properties();
        properties.putAll(newSecrets);
        if (persistent) {
            try (Writer writer = Files.newBufferedWriter(vaultFile)) {
                properties.store(writer, null);
            } catch (IOException e) {
                return Result.failure(e.getMessage());
            }
        }
        secrets.set(newSecrets);
        return Result.success();
    }
}
