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
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation that returns private keys stored in a vault. If the key is not found in the vault, this implementation
 * falls back to the {@link Config} attempting to get the private key from there.
 * <p>
 * Note that storing private key material in the config is <strong>NOT SECURE</strong> and should be avoided in production scenarios!
 */
public class VaultPrivateKeyResolver extends AbstractPrivateKeyResolver {

    private final Vault vault;
    private final Monitor monitor;
    private final Config config;

    public VaultPrivateKeyResolver(KeyParserRegistry registry, Vault vault, Monitor monitor, Config config) {
        super(registry);
        this.vault = vault;
        this.monitor = monitor;
        this.config = config;
    }

    @Override
    protected @Nullable String resolveInternal(String keyId) {
        var privateKey = vault.resolveSecret(keyId);
        if (privateKey == null) { //fallback
            monitor.debug("Private Key not found in vault, fallback to config.");
            privateKey = resolveFromConfig(keyId);
        }
        return privateKey;
    }

    private String resolveFromConfig(String keyId) {
        return config.getString(keyId, null);
    }
}
