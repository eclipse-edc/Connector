/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

/**
 * Default implementation of {@link ParticipantVault}, that doesn't actually implement a multi-tenant vault, instead it delegates down
 * to a {@link Vault} instance. The {@code vaultPartition} parameter is ignored.
 *
 * @param vault   The {@link Vault} that is used as backing secret store.
 * @param monitor The {@link Monitor} that is used to log debug messages, whenever an operation is executed on it.
 */
public record DefaultParticipantVaultImpl(Vault vault, Monitor monitor) implements ParticipantVault {

    @Override
    public String resolveSecret(String vaultPartition, String key) {
        warn("resolve");
        return vault.resolveSecret(key);
    }

    @Override
    public Result<Void> storeSecret(String vaultPartition, String key, String value) {
        warn("store");
        return vault.storeSecret(key, value);
    }

    @Override
    public Result<Void> deleteSecret(String vaultPartition, String key) {
        warn("delete");
        return vault.deleteSecret(key);
    }

    private void warn(String operation) {
        monitor.debug("A '%s' operation was executed on the default ParticipantVault. Please consider using an actual multi-user-capable implementation!".formatted(operation));
    }

}
