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

package org.eclipse.edc.catalog.cache.query;

import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

public class QueryServiceImpl implements QueryService {

    private final FederatedCatalogCache cache;

    public QueryServiceImpl(FederatedCatalogCache cache) {
        this.cache = cache;
    }

    @Override
    public ServiceResult<Collection<Catalog>> getCatalog(QuerySpec query) {

        return ServiceResult.from(Result.ofThrowable(() -> cache.query(query)));
    }
}
