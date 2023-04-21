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

package org.eclipse.edc.connector.api.management.catalog;

import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static java.util.Optional.ofNullable;

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
    @POST
    @Path("/request")
    public void requestCatalog(@Valid @jakarta.validation.constraints.NotNull CatalogRequestDto requestDto, @Suspended AsyncResponse response) {

        var result = transformerRegistry.transform(ofNullable(requestDto.getQuerySpec()).orElse(QuerySpecDto.Builder.newInstance().build()), QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }
        performQuery(requestDto.getProviderUrl(), result.getContent(), response);
    }

    private void performQuery(String providerUrl, QuerySpec spec, AsyncResponse response) {
        service.getByProviderUrl(providerUrl, spec)
                .whenComplete((content, throwable) -> {
                    if (throwable == null) {
                        response.resume(content);
                    } else {
                        if (throwable instanceof EdcException || throwable.getCause() instanceof EdcException) {
                            response.resume(new BadGatewayException(throwable.getMessage()));
                        } else {
                            response.resume(throwable);
                        }
                    }
                });
    }
}
