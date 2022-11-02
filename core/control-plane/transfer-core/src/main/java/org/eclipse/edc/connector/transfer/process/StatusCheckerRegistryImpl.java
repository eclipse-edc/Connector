/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.transfer.process;

import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.types.StatusChecker;

import java.util.HashMap;
import java.util.Map;

public class StatusCheckerRegistryImpl implements StatusCheckerRegistry {
    private final Map<String, StatusChecker> inMemoryMap;

    public StatusCheckerRegistryImpl() {
        inMemoryMap = new HashMap<>();
    }

    @Override
    public void register(String destinationType, StatusChecker statusChecker) {
        inMemoryMap.put(destinationType, statusChecker);
    }

    @Override
    public StatusChecker resolve(String destinationType) {
        return inMemoryMap.get(destinationType);
    }
}
