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

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.Collection;
import java.util.List;

/**
 * Internal datastore where all the catalogs from all the other connectors are stored by the FederatedCatalogCache.
 */
public interface FederatedCacheStore {

    /**
     * Adds an {@link ContractOffer} to the store
     */
    void save(ContractOffer asset);

    /**
     * Queries the store for {@link ContractOffer}s
     *
     * @param query A list of criteria the asset must fulfill
     * @return A collection of assets that are already in the store and that satisfy a given list of criteria.
     */
    Collection<ContractOffer> query(List<Criterion> query);

    /**
     * Deletes all entries from the cache
     */
    void deleteAll();

}