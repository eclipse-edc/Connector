/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v2/catalog")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class CatalogApiController implements CatalogApi {

    private final CatalogService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;

    public CatalogApiController(CatalogService service, TypeTransformerRegistry transformerRegistry, JsonLd jsonLd) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.jsonLd = jsonLd;
    }

    @Override
    @POST
    @Path("/request")
    public void requestCatalog(JsonObject requestBody, @Suspended AsyncResponse response) {
        var request = jsonLd.expand(requestBody)
                .compose(expanded -> transformerRegistry.transform(expanded, CatalogRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, CatalogRequest.class))
                .orElseThrow(InvalidRequestException::new);

        service.request(request.getProviderUrl(), request.getProtocol(), request.getQuerySpec())
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
