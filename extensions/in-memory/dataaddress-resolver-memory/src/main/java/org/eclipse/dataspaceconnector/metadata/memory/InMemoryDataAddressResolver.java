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

import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An ephemeral asset index.
 */
public class InMemoryDataAddressResolver implements DataAddressResolver {
    private final Map<String, DataAddress> dataAddresses = new ConcurrentHashMap<>();

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId, "assetId");
        if (!dataAddresses.containsKey(assetId) || dataAddresses.get(assetId) == null) {
            throw new IllegalArgumentException("No DataAddress found for Asset ID=" + assetId);
        }
        return dataAddresses.get(assetId);
    }

    public void add(String id, DataAddress address) {
        dataAddresses.put(id, address);
    }

}
