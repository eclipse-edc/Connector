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

package org.eclipse.edc.connector.core.vault;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryVault implements Vault {
    private final Map<String, String> secrets = new ConcurrentHashMap<>();
    private final Monitor monitor;

    public InMemoryVault(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public @Nullable String resolveSecret(String s) {
        monitor.debug("resolving secret " + s);
        if (s == null) {
            monitor.warning("Secret name is null - skipping");
            return null;
        }
        return secrets.getOrDefault(s, null);
    }

    @Override
    public Result<Void> storeSecret(String s, String s1) {
        monitor.debug("storing secret " + s);
        secrets.put(s, s1);
        return Result.success();
    }

    @Override
    public Result<Void> deleteSecret(String s) {
        monitor.debug("deleting secret " + s);
        return secrets.remove(s) == null ?
                Result.failure("Secret with key " + s + " does not exist") :
                Result.success();
    }
}
