/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.federatedcatalog.util;

import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FederatedCatalogUtilTest {

    @Test
    void flatten() {
        var rootCatalog = createCatalog("root")
                .dataset(createCatalog("sub1").build())
                .dataset(createCatalog("sub2")
                        .dataset(createCatalog("subsub1").build())
                        .build())
                .build();

        var flattened = FederatedCatalogUtil.flatten(rootCatalog);

        assertThat(flattened.getDatasets()).hasSize(8);
        assertThat(flattened.getDataServices()).hasSize(4);
    }

    @Test
    void flatten_noHierarchy() {
        var rootCatalog = createCatalog("root").build();

        var flattened = FederatedCatalogUtil.flatten(rootCatalog);

        assertThat(flattened).usingRecursiveComparison().isEqualTo(rootCatalog);
    }

    @Test
    void merge() {
        var catalog1 = createCatalog("cat1").build();
        var catalog2 = createCatalog("cat2").build();

        var merged = FederatedCatalogUtil.merge(catalog1, catalog2);

        assertThat(merged.getDatasets()).hasSize(4);
        assertThat(merged.getDataServices()).hasSize(2);
    }

    @Test
    void copy() {
        var catalog1 = createCatalog("cat1").build();
        var copy = FederatedCatalogUtil.copy(catalog1).build();

        assertThat(catalog1).usingRecursiveComparison().isEqualTo(copy);
    }

    @Test
    void copy_withDatasets() {
        var catalog1 = createCatalog("cat1").build();
        var datasets = List.of(Dataset.Builder.newInstance().id("new-dataset").build());

        var copy = FederatedCatalogUtil.copy(catalog1, datasets).build();

        assertThat(catalog1.getDataServices()).isEqualTo(copy.getDataServices());
        assertThat(catalog1.getParticipantId()).isEqualTo(copy.getParticipantId());
        assertThat(copy.getDatasets()).isEqualTo(datasets).doesNotContain(catalog1.getDatasets().toArray(new Dataset[0]));
    }

    private Catalog.Builder createCatalog(String prefix) {
        return Catalog.Builder.newInstance()
                .id(prefix)
                .dataService(DataService.Builder.newInstance().id(prefix + "-service").build())
                .dataset(Dataset.Builder.newInstance().id(prefix + "-dataset-1").build())
                .dataset(Dataset.Builder.newInstance().id(prefix + "-dataset-2").build());
    }
}