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
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.catalog.spi.DistributionResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class DataServiceRegistryImpl implements DataServiceRegistry, DistributionResolver {

    private final Map<DataService, DistributionResolver> dataServices = new HashMap<>();

    @Override
    public void register(DataService dataService, DistributionResolver distributionResolver) {
        dataServices.put(dataService, distributionResolver);
    }

    @Override
    public List<DataService> getDataServices() {
        return new ArrayList<>(dataServices.keySet());
    }

    @Override
    public List<Distribution> getDistributions() {
        return dataServices.values().stream().flatMap(it -> it.getDistributions().stream()).collect(toList());
    }
}
