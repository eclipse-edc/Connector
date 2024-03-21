/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Implementation that returns private keys stored in a vault. If the key is not found in the vault, this implementation
 * falls back to the {@link Config} attempting to get the private key from there.
 * <p>
 * Note that storing private key material in the config is <strong>NOT SECURE</strong> and should be avoided in production scenarios!
 */
public class VaultPrivateKeyResolver extends AbstractPrivateKeyResolver {

    private final Vault vault;

    public VaultPrivateKeyResolver(KeyParserRegistry registry, Vault vault, Monitor monitor, Config config) {
        super(registry, config, monitor);
        this.vault = vault;
    }

    @NotNull
    @Override
    protected Result<String> resolveInternal(String keyId) {
        return Optional.ofNullable(vault.resolveSecret(keyId))
                .map(Result::success)
                .orElseGet(() -> Result.failure("Private key with ID '%s' not found in Vault".formatted(keyId)));
    }
}
