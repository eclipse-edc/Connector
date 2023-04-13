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

package org.eclipse.edc.catalog.spi;

import java.util.List;

/**
 * {@link DataService} registry. A {@link DataService} is a dataspace component that offers data.
 */
public interface DataServiceRegistry {

    /**
     * Register a {@link DataService} with its {@link DistributionResolver}.
     *
     * @param dataService the Data Service
     * @param distributionResolver the Distribution Resolver
     */
    void register(DataService dataService, DistributionResolver distributionResolver);

    /**
     * Returns all the {@link DataService}s
     *
     * @return a list of Data Services. Always not null
     */
    List<DataService> getDataServices();
}
