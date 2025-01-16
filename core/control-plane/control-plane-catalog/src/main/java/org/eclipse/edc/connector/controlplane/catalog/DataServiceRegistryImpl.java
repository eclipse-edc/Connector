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

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataServiceRegistryImpl implements DataServiceRegistry {

    private final Map<String, List<DataService>> dataServices = new ConcurrentHashMap<>();

    @Override
    public void register(String protocol, DataService dataService) {
        dataServices.computeIfAbsent(protocol, k -> new ArrayList<>()).add(dataService);
    }

    @Override
    public List<DataService> getDataServices(String protocol) {
        return dataServices.computeIfAbsent(protocol, k -> new ArrayList<>());
    }

}
