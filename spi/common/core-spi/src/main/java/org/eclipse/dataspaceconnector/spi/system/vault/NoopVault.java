/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system.vault;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

public class NoopVault implements Vault {
    @Override
    public @Nullable String resolveSecret(String key) {
        return null;
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        return Result.success();
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        return Result.success();
    }
}
