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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.connector.catalog;

import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultDistributionResolver implements DistributionResolver {

    private final DataServiceRegistry dataServiceRegistry;
    private final DataPlaneInstanceStore dataPlaneInstanceStore;

    public DefaultDistributionResolver(DataServiceRegistry dataServiceRegistry, DataPlaneInstanceStore dataPlaneInstanceStore) {
        this.dataServiceRegistry = dataServiceRegistry;
        this.dataPlaneInstanceStore = dataPlaneInstanceStore;
    }

    @Override
    public List<Distribution> getDistributions(Asset asset, DataAddress dataAddress) {
        return dataPlaneInstanceStore.getAll()
                .flatMap(it -> it.getAllowedDestTypes().stream())
                .map(this::createDistribution)
                .collect(Collectors.toList());
    }
    
    private Distribution createDistribution(String format) {
        var builder = Distribution.Builder.newInstance().format(format);
        dataServiceRegistry.getDataServices().forEach(builder::dataService);
        return builder.build();
    }
}
