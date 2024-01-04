/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Optional;

public class VaultPublicKeyResolver extends AbstractPublicKeyResolver {
    private final Vault vault;

    public VaultPublicKeyResolver(KeyParserRegistry registry, Config config, Monitor monitor, Vault vault) {
        super(registry, config, monitor);
        this.vault = vault;
    }

    @Override
    protected Result<String> resolveInternal(String id) {
        return Optional.ofNullable(vault.resolveSecret(id))
                .map(Result::success)
                .orElseGet(() -> Result.failure("Private key with ID '%s' not found in Vault".formatted(id)));
    }
}
