/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.crawler.store;

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNodeDirectory implements TargetNodeDirectory {
    private final Map<String, Map<String, TargetNode>> cache = new ConcurrentHashMap<>();

    @Override
    public List<TargetNode> getAll(String participantContextId) {
        return List.copyOf(partition(participantContextId).values()); //never return the internal copy
    }

    @Override
    public void insert(TargetNode node) {
        partition(node.participantContextId()).put(node.id(), node);
    }

    @Override
    public TargetNode remove(String participantContextId, String id) {
        return partition(participantContextId).remove(id);
    }

    private Map<String, TargetNode> partition(String participantContextId) {
        return cache.computeIfAbsent(participantContextId, k -> new ConcurrentHashMap<>());
    }
}
