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

package org.eclipse.edc.connector.controlplane.catalog.spi;

import java.util.List;

/**
 * {@link DataService} registry. A {@link DataService} is a dataspace component that offers data.
 */
public interface DataServiceRegistry {

    /**
     * Register a {@link DataService} with its {@link DistributionResolver}.
     *
     * @param protocol    the protocol
     * @param dataService the Data Service
     */
    void register(String protocol, DataService dataService);

    /**
     * Returns all the {@link DataService}s
     *
     * @param protocol the protocol
     * @return a list of Data Services. Always not null
     */
    List<DataService> getDataServices(String protocol);
}
