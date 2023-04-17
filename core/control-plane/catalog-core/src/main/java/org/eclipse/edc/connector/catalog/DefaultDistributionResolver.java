/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.catalog;

import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultDistributionResolver implements DistributionResolver {

    private final DataService dataService;
    private final DataPlaneInstanceStore dataPlaneInstanceStore;

    public DefaultDistributionResolver(DataService dataService, DataPlaneInstanceStore dataPlaneInstanceStore) {
        this.dataService = dataService;
        this.dataPlaneInstanceStore = dataPlaneInstanceStore;
    }

    @Override
    public List<Distribution> getDistributions(Asset asset, DataAddress dataAddress) {
        return dataPlaneInstanceStore.getAll()
                .flatMap(it -> it.getAllowedDestTypes().stream())
                .map(destType -> Distribution.Builder.newInstance().format(destType).dataService(dataService).build())
                .collect(Collectors.toList());
    }
}
