/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DefaultDistributionResolverTest {

    private final DataService dataService = DataService.Builder.newInstance().build();
    private final DataPlaneInstanceStore dataPlaneInstanceStore = Mockito.mock(DataPlaneInstanceStore.class);

    private final DefaultDistributionResolver resolver = new DefaultDistributionResolver(dataService, dataPlaneInstanceStore);

    @Test
    void shouldReturnDistributionForEverySupportedDestType() {
        var dataPlane1 = DataPlaneInstance.Builder.newInstance().url("http://data-plane-one").allowedDestType("type1").build();
        var dataPlane2 = DataPlaneInstance.Builder.newInstance().url("http://data-plane-two").allowedDestType("type2").build();
        when(dataPlaneInstanceStore.getAll()).thenReturn(Stream.of(dataPlane1, dataPlane2));
        var asset = Asset.Builder.newInstance().build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        var distributions = resolver.getDistributions(asset, dataAddress);

        assertThat(distributions).hasSize(2)
                .anySatisfy(distribution -> {
                    assertThat(distribution.getFormat()).isEqualTo("type1");
                    assertThat(distribution.getDataService()).isSameAs(dataService);
                })
                .anySatisfy(distribution -> {
                    assertThat(distribution.getFormat()).isEqualTo("type2");
                    assertThat(distribution.getDataService()).isSameAs(dataService);
                });
    }
}
