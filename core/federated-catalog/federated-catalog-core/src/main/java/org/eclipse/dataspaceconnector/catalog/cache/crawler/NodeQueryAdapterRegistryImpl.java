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

package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeQueryAdapterRegistryImpl implements NodeQueryAdapterRegistry {

    private final Map<String, List<NodeQueryAdapter>> map;

    public NodeQueryAdapterRegistryImpl() {
        map = new ConcurrentHashMap<>();
    }


    @Override
    public Collection<NodeQueryAdapter> findForProtocol(String protocolName) {
        if (!map.containsKey(protocolName)) {
            return Collections.emptyList();
        }
        return map.get(protocolName);
    }

    @Override
    public void register(String protocolName, NodeQueryAdapter adapter) {
        if (!map.containsKey(protocolName)) {
            map.put(protocolName, new ArrayList<>());
        }
        map.get(protocolName).add(adapter);
    }

    @Override
    public void unregister(String protocolName, NodeQueryAdapter adapter) {
        if (map.containsKey(protocolName)) {
            map.get(protocolName).remove(adapter);

            if (map.get(protocolName).isEmpty()) {
                map.remove(protocolName);
            }
        }
    }
}
