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

import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class InMemoryVault implements Vault {
    private static final String DEFAULT_PARTITION = "default";
    private final Map<String, Map<String, String>> secrets = new ConcurrentHashMap<>();
    private final Monitor monitor;
    private final ParticipantContextSupplier participantContextSupplier;

    public InMemoryVault(Monitor monitor, ParticipantContextSupplier participantContextSupplier) {
        this.monitor = monitor;
        this.participantContextSupplier = participantContextSupplier;
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return resolveSecret(getPartition(), key);
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        return storeSecret(getPartition(), key, value);
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        return deleteSecret(getPartition(), key);
    }

    @Override
    public @Nullable String resolveSecret(String vaultPartition, String s) {
        vaultPartition = ofNullable(vaultPartition).orElse(getPartition());

        monitor.debug("Resolving secret " + s);
        if (s == null) {
            monitor.warning("Secret name is null - skipping");
            return null;
        }
        return ofNullable(secrets.get(vaultPartition)).map(map -> map.getOrDefault(s, null)).orElse(null);
    }

    @Override
    public Result<Void> storeSecret(String vaultPartition, String s, String s1) {
        vaultPartition = ofNullable(vaultPartition).orElse(getPartition());
        monitor.debug("Storing secret " + s);

        var partition = secrets.computeIfAbsent(vaultPartition, k -> new ConcurrentHashMap<>());
        partition.put(s, s1);
        return Result.success();
    }

    @Override
    public Result<Void> deleteSecret(String vaultPartition, String s) {
        vaultPartition = ofNullable(vaultPartition).orElse(getPartition());
        monitor.debug("Deleting secret " + s);

        var result = ofNullable(secrets.get(vaultPartition)).map(map -> map.remove(s)).orElse(null);

        return result == null ?
                Result.failure("Secret with key " + s + " does not exist") :
                Result.success();
    }

    private @NotNull String getPartition() {
        return Optional.ofNullable(participantContextSupplier)
                .map(Supplier::get).filter(ServiceResult::succeeded).map(ServiceResult::getContent)
                .map(ParticipantContext::getParticipantContextId).orElse(DEFAULT_PARTITION);
    }
}
