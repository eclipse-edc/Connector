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

import org.eclipse.edc.connector.catalog.spi.DataService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataServiceRegistryImplTest {

    private final DataServiceRegistryImpl registry = new DataServiceRegistryImpl();

    @Test
    void shouldReturnRegisteredDataService() {
        var dataService = DataService.Builder.newInstance().build();

        registry.register(dataService);
        var dataServices = registry.getDataServices();

        assertThat(dataServices).containsExactly(dataService);
    }

}
