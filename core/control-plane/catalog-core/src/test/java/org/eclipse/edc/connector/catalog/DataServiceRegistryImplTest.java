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
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataServiceRegistryImplTest {

    private final DataServiceRegistryImpl registry = new DataServiceRegistryImpl();

    @Test
    void shouldReturnRegisteredDataService() {
        var dataService = DataService.Builder.newInstance().build();

        registry.register(dataService, Collections::emptyList);
        var dataServices = registry.getDataServices();

        assertThat(dataServices).containsExactly(dataService);
    }

    @Test
    void shouldReturnDistributions() {
        var dataService1 = DataService.Builder.newInstance().build();
        var distribution1 = Distribution.Builder.newInstance().dataService(dataService1).format("format1").build();
        var dataService2 = DataService.Builder.newInstance().build();
        var distribution2 = Distribution.Builder.newInstance().dataService(dataService2).format("format2").build();
        registry.register(dataService1, () -> List.of(distribution1));
        registry.register(dataService2, () -> List.of(distribution2));

        var distributions = registry.getDistributions();

        assertThat(distributions).containsExactlyInAnyOrder(distribution1, distribution2);
    }
}
