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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;

@Path("/catalog")
@Produces({ MediaType.APPLICATION_JSON })
public class CatalogApiController implements CatalogApi {

    private final CatalogService service;

    public CatalogApiController(CatalogService service) {
        this.service = service;
    }

    @Override
    @GET
    public void getCatalog(@QueryParam("providerUrl") String providerUrl, @Suspended AsyncResponse response) {
        service.getByProviderUrl(providerUrl)
                .whenComplete((content, throwable) -> {
                    if (throwable == null) {
                        response.resume(content);
                    } else {
                        response.resume(throwable);
                    }
                });
    }
}
