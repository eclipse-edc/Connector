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

public record DefaultParticipantVaultImpl(Vault vault, Monitor monitor) implements ParticipantVault {

    @Override
    public String resolveSecret(String participantContextId, String key) {
        warn("resolve");
        return vault.resolveSecret(key);
    }

    @Override
    public Result<Void> storeSecret(String participantContextId, String key, String value) {
        warn("store");
        return vault.storeSecret(key, value);
    }

    @Override
    public Result<Void> deleteSecret(String participantContextId, String key) {
        warn("delete");
        return vault.deleteSecret(key);
    }

    private void warn(String operation) {
        monitor.warning("A '%s' operation was executed on the default ParticipantVault. Please consider using an actual multi-user-capable implementation!".formatted(operation));
    }

}
