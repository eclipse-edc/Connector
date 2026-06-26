/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.sts.registry;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecureTokenServiceRegistryImpl implements SecureTokenServiceRegistry {

    private final Map<String, SecureTokenService> entries = new ConcurrentHashMap<>();

    @Override
    public void register(String type, SecureTokenService secureTokenService) {
        entries.put(type, secureTokenService);
    }

    @Override
    public @Nullable SecureTokenService resolve(String type) {
        return entries.get(type);
    }
}
