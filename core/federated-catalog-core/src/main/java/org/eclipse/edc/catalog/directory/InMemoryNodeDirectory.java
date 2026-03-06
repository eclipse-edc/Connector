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

package org.eclipse.edc.catalog.directory;

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNodeDirectory implements TargetNodeDirectory {
    private final Map<String, TargetNode> cache = new ConcurrentHashMap<>();

    @Override
    public List<TargetNode> getAll() {
        return List.copyOf(cache.values()); //never return the internal copy
    }

    @Override
    public void insert(TargetNode node) {
        cache.put(node.id(), node);
    }

    @Override
    public TargetNode remove(String id) {
        return cache.remove(id);
    }
}
