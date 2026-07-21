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

package org.eclipse.edc.connector.controlplane.defaults.strategy;

import org.eclipse.edc.connector.controlplane.dataplane.spi.strategy.SelectionStrategy;
import org.eclipse.edc.connector.controlplane.dataplane.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultSelectionStrategyRegistry implements SelectionStrategyRegistry {
    private final LockManager lockManager;

    private final Map<String, SelectionStrategy> strategyMap;


    public DefaultSelectionStrategyRegistry() {
        lockManager = new LockManager(new ReentrantReadWriteLock(true));
        strategyMap = new HashMap<>();
    }

    @Override
    public SelectionStrategy find(String strategyName) {
        return lockManager.readLock(() -> strategyMap.get(strategyName));
    }

    @Override
    public void add(SelectionStrategy strategy) {
        lockManager.writeLock(() -> strategyMap.put(strategy.getName(), strategy));
    }

    @Override
    public Collection<String> getAll() {
        return lockManager.readLock(() -> new ArrayList<>(strategyMap.keySet()));
    }
}
