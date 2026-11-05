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
 * <p>
 * Every entry is owned by the local {@code participantContextId} that crawled it, so the same remote catalog can be
 * cached independently for several participant contexts hosted by the same runtime.
 */
@ExtensionPoint
public interface FederatedCatalogCache {

    /**
     * Adds a {@link Catalog} to the store, owned by the given participant context.
     *
     * @param participantContextId the local participant context that crawled the catalog
     * @param catalog              the catalog to store
     */
    void save(String participantContextId, Catalog catalog);

    /**
     * Queries the store for the {@link Catalog}s owned by the given participant context.
     *
     * @param participantContextId the local participant context whose catalogs should be queried
     * @param query                A list of criteria the catalog must fulfill
     * @return A collection of catalogs that are already in the store and that satisfy a given list of criteria.
     */
    Collection<Catalog> query(String participantContextId, QuerySpec query);

    /**
     * Deletes all entries owned by the given participant context that are marked as "expired"
     *
     * @param participantContextId the local participant context whose expired entries should be deleted
     */
    void deleteExpired(String participantContextId);

    /**
     * Marks all entries owned by the given participant context as "expired", i.e. marks them for deletion
     *
     * @param participantContextId the local participant context whose entries should be marked for deletion
     */
    void expireAll(String participantContextId);
}
