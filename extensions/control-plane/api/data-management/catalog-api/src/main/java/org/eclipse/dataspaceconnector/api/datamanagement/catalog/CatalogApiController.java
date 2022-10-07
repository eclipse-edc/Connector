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

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.model.CatalogRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;

@Path("/catalog")
@Produces({ MediaType.APPLICATION_JSON })
public class CatalogApiController implements CatalogApi {

    private final CatalogService service;
    private final DtoTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public CatalogApiController(CatalogService service, DtoTransformerRegistry transformerRegistry, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @Override
    @GET
    public void getCatalog(@jakarta.validation.constraints.NotNull(message = PROVIDER_URL_NOT_NULL_MESSAGE) @QueryParam("providerUrl") String providerUrl, @Valid @BeanParam QuerySpecDto querySpecDto, @Suspended AsyncResponse response) {

        @NotNull QuerySpec spec;
        if (querySpecDto != null) {
            var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
            if (result.failed()) {
                throw new InvalidRequestException(result.getFailureMessages());
            }
            spec = result.getContent();
        } else {
            spec = QuerySpec.max();
            monitor.debug("No paging parameters were supplied, using 0...Integer.MAX_VALUE");
        }

        performQuery(providerUrl, spec, response);
    }

    @Override
    @POST
    @Path("/request")
    public void requestCatalog(@RequestBody(required = true) CatalogRequestDto requestDto, @Suspended AsyncResponse response) {

        var query = QuerySpec.Builder.newInstance()
                .filter(requestDto.getFilter())
                .limit(requestDto.getLimit())
                .offset(requestDto.getOffset())
                .sortOrder(requestDto.getSortOrder())
                .sortField((requestDto.getSortField()))
                .build();

        performQuery(requestDto.getProviderUrl(), query, response);
    }

    private void performQuery(String providerUrl, QuerySpec spec, AsyncResponse response) {
        service.getByProviderUrl(providerUrl, spec)
                .whenComplete((content, throwable) -> {
                    if (throwable == null) {
                        response.resume(content);
                    } else {
                        response.resume(throwable);
                    }
                });
    }
}
