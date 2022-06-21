/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Adapter to translate a {@link FederatedCatalogCacheQuery} into whatever query language the underlying data store uses.
 * This is the main interface to perform a query against the internal Federated Cache, however, queries ({@link FederatedCatalogCacheQuery})
 * should be submitted to the {@link CacheQueryAdapterRegistry}.
 * <p>
 * Implement this interface in your extension to contribute another "database protocol", e.g. a {@link FederatedCacheStore} based on
 * PostgreSQL.
 */
public interface CacheQueryAdapter {
    /**
     * Executes the query.
     *
     * @return A stream of {@link Asset} objects. Can be empty, can never be null.
     * @throws IllegalArgumentException may be thrown if the implementor cannot translate the query.
     */
    @NotNull Stream<ContractOffer> executeQuery(FederatedCatalogCacheQuery query);

    /**
     * Checks whether a given query can be run by the implementor. This does not limit itself to whether the query
     * is actually translatable, this could even go as far as perform semantic checks on the query.
     *
     * @param query The Query
     * @return true if the query can be run, false otherwise.
     */
    boolean canExecute(FederatedCatalogCacheQuery query);

}
