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
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Accepts a {@link FederatedCatalogCacheQuery} and fetches a collection of {@link Asset} that conform to that query.
 */
@FunctionalInterface
@Feature(QueryEngine.FEATURE)
public interface QueryEngine {
    String FEATURE = "edc:catalog:query:engine";

    QueryResponse getCatalog(FederatedCatalogCacheQuery query);
}
