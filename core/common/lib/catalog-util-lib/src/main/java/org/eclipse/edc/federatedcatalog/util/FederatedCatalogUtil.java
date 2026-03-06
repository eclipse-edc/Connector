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
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class FederatedCatalogUtil {
    /**
     * Merges two catalogs by adding all datasets, dataservices, properties and distributions of the source into the target
     */
    public static Catalog merge(Catalog destination, Catalog source) {
        return copy(destination)
                .datasets(source.getDatasets())
                .dataServices(Stream.concat(destination.getDataServices().stream(), source.getDataServices().stream()).toList())
                .properties(source.getProperties())
                .distributions(source.getDistributions())
                .build();
    }

    /**
     * Creates an exact copy of the given catalog
     */
    public static Catalog.Builder copy(Catalog catalog) {
        return Catalog.Builder.newInstance().id(catalog.getId())
                .participantId(catalog.getParticipantId())
                .properties(ofNullable(catalog.getProperties()).orElseGet(HashMap::new))
                .dataServices(ofNullable(catalog.getDataServices()).orElseGet(ArrayList::new))
                .distributions(ofNullable(catalog.getDistributions()).orElseGet(ArrayList::new))
                .datasets(ofNullable(catalog.getDatasets()).orElseGet(ArrayList::new));
    }

    /**
     * Creates an exact copy of the given catalog, but uses the given list of Datasets instead of the original's
     */
    public static Catalog.Builder copy(Catalog catalog, List<Dataset> datasets) {
        return Catalog.Builder.newInstance().id(catalog.getId())
                .participantId(catalog.getParticipantId())
                .properties(ofNullable(catalog.getProperties()).orElseGet(HashMap::new))
                .dataServices(ofNullable(catalog.getDataServices()).orElseGet(ArrayList::new))
                .distributions(ofNullable(catalog.getDistributions()).orElseGet(ArrayList::new))
                .datasets(datasets);
    }

    /**
     * Takes a catalog that may contain subcatalogs and flattens the list of datasets etc.
     */
    public static Catalog flatten(Catalog rootCatalog) {
        var datasets = getDatasets(rootCatalog, Dataset.class); // will add nested catalogs later
        var flattenedCatalog = FederatedCatalogUtil.copy(rootCatalog, datasets).build(); // remove all sub-catalogs

        var subCatalogs = getDatasets(rootCatalog, Catalog.class);

        // recursively merge every sub-catalog with the root catalog
        if (!subCatalogs.isEmpty()) {
            return subCatalogs.stream()
                    .map(FederatedCatalogUtil::flatten)
                    .filter(Objects::nonNull)
                    .reduce(FederatedCatalogUtil::merge)
                    .map(c -> merge(flattenedCatalog, c))
                    .orElse(null);
        }

        return flattenedCatalog;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Dataset> List<T> getDatasets(Catalog catalog, Class<T> datasetType) {
        var partitions = catalog.getDatasets().stream().collect(Collectors.groupingBy(Dataset::getClass));
        return ofNullable(partitions.get(datasetType))
                .map(datasets -> datasets.stream().map(d -> (T) d).toList())
                .orElse(new ArrayList<>());
    }
}
