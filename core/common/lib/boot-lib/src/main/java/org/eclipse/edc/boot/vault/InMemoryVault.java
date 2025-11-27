/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.boot.vault;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

public class InMemoryVault implements Vault {
    private static final String DEFAULT_PARTITION = "default";
    private final Map<String, Map<String, String>> secrets = new ConcurrentHashMap<>();
    private final Monitor monitor;

    public InMemoryVault(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return resolveSecret(DEFAULT_PARTITION, key);
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        return storeSecret(DEFAULT_PARTITION, key, value);
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        return deleteSecret(DEFAULT_PARTITION, key);
    }

    @Override
    public @Nullable String resolveSecret(String vaultPartition, String s) {
        vaultPartition = ofNullable(vaultPartition).orElse(DEFAULT_PARTITION);

        monitor.debug("Resolving secret " + s);
        if (s == null) {
            monitor.warning("Secret name is null - skipping");
            return null;
        }
        return ofNullable(secrets.get(vaultPartition)).map(map -> map.getOrDefault(s, null)).orElse(null);
    }

    @Override
    public Result<Void> storeSecret(String vaultPartition, String s, String s1) {
        vaultPartition = ofNullable(vaultPartition).orElse(DEFAULT_PARTITION);
        monitor.debug("Storing secret " + s);

        var partition = secrets.computeIfAbsent(vaultPartition, k -> new ConcurrentHashMap<>());
        partition.put(s, s1);
        return Result.success();
    }

    @Override
    public Result<Void> deleteSecret(String vaultPartition, String s) {
        vaultPartition = ofNullable(vaultPartition).orElse(DEFAULT_PARTITION);
        monitor.debug("Deleting secret " + s);

        var result = ofNullable(secrets.get(vaultPartition)).map(map -> map.remove(s)).orElse(null);

        return result == null ?
                Result.failure("Secret with key " + s + " does not exist") :
                Result.success();
    }
}
