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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryNodeDirectory implements FederatedCacheNodeDirectory {
    private final List<FederatedCacheNode> cache = new CopyOnWriteArrayList<>();

    @Override
    public List<FederatedCacheNode> getAll() {
        return List.copyOf(cache); //never return the internal copy
    }

    @Override
    public void insert(FederatedCacheNode node) {
        cache.add(node);
    }
}
