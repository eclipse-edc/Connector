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

package org.eclipse.edc.catalog.spi;

import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Collection;

/**
 * Internal datastore where all the catalogs from all the other connectors are stored by the FederatedCatalogCache.
 */
@ExtensionPoint
public interface FederatedCatalogCache {

    /**
     * Adds an {@code ContractOffer} to the store
     */
    void save(Catalog catalog);

    /**
     * Queries the store for {@code ContractOffer}s
     *
     * @param query A list of criteria the asset must fulfill
     * @return A collection of assets that are already in the store and that satisfy a given list of criteria.
     */
    Collection<Catalog> query(QuerySpec query);

    /**
     * Deletes all entries from the cache that are marked as "expired"
     */
    void deleteExpired();

    /**
     * Marks all entries as "expired", i.e. marks them for deletion
     */
    void expireAll();
}
