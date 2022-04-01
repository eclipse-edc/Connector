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

package org.eclipse.dataspaceconnector.catalog.cache.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryException;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryNotAcceptedException;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.Collection;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/")
public class CatalogController {

    private final Monitor monitor;
    private final QueryEngine queryEngine;

    public CatalogController(Monitor monitor, QueryEngine queryEngine) {
        this.monitor = monitor;
        this.queryEngine = queryEngine;
    }

    @POST
    @Path("catalog")
    public Collection<ContractOffer> getCatalog(FederatedCatalogCacheQuery federatedCatalogCacheQuery) {
        monitor.info("Received a catalog request");
        var queryResponse = queryEngine.getCatalog(federatedCatalogCacheQuery);
        // query not possible
        if (queryResponse.getStatus() == QueryResponse.Status.NO_ADAPTER_FOUND) {
            throw new QueryNotAcceptedException();
        }
        if (!queryResponse.getErrors().isEmpty()) {
            throw new QueryException(queryResponse.getErrors());
        }

        return queryResponse.getOffers();
    }
}
