/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implements a vault backed by Hashicorp Vault.
 */
public class HashicorpVault implements Vault {

    @NotNull
    private final HashicorpVaultClient hashicorpVaultClient;
    @NotNull
    private final Monitor monitor;

    public HashicorpVault(@NotNull HashicorpVaultClient hashicorpVaultClient, @NotNull Monitor monitor) {
        this.hashicorpVaultClient = hashicorpVaultClient;
        this.monitor = monitor;
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        var result = hashicorpVaultClient.getSecretValue(key);

        return result.succeeded() ? result.getContent() : null;
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        var result = hashicorpVaultClient.setSecret(key, value);

        return result.succeeded() ? Result.success() : Result.failure(result.getFailureMessages());
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        return hashicorpVaultClient.destroySecret(key);
    }
}
