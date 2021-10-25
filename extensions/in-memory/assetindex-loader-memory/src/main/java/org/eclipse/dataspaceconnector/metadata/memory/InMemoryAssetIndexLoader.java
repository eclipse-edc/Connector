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

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An ephemeral asset index loader. Has dependency onto the {@link InMemoryAssetIndex} and the {@link InMemoryDataAddressResolver}
 */
public class InMemoryAssetIndexLoader implements AssetIndexLoader {
    private final InMemoryAssetIndex index;
    private final InMemoryDataAddressResolver resolver;
    private final ReentrantLock lock;

    public InMemoryAssetIndexLoader(InMemoryAssetIndex index, InMemoryDataAddressResolver resolver) {
        this.index = index;
        this.resolver = resolver;
        lock = new ReentrantLock();
    }

    @Override
    public void insert(Asset asset, DataAddress address) {
        lock.lock();
        add(asset, address);
        lock.unlock();
    }

    @Override
    public void insertAll(Map<Asset, DataAddress> entries) {
        lock.lock();
        entries.forEach(this::add);
        lock.unlock();
    }

    private void add(Asset asset, DataAddress address) {
        index.add(asset);
        resolver.add(asset.getId(), address);
    }
}

