/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.junit.launcher;

import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory vault for testing.
 */
public class MockVault implements Vault {
    private final Map<String, String> secrets = new ConcurrentHashMap<>();

    @Override
    public @Nullable String resolveSecret(String key) {
        return secrets.get(key);
    }

    @Override
    public VaultResponse storeSecret(String key, String value) {
        secrets.put(key, value);
        return VaultResponse.OK;
    }

    @Override
    public VaultResponse deleteSecret(String key) {
        secrets.remove(key);
        return VaultResponse.OK;
    }
}
