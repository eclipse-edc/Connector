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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("/catalog")
@Produces({ MediaType.APPLICATION_JSON })
public class CatalogApiController implements CatalogApi {
    private static final String EXTRA_PROPERTY_HEADER_PREFIX = "property_";

    private final CatalogService service;

    public CatalogApiController(CatalogService service) {
        this.service = service;
    }

    @Override
    @GET
    public void getCatalog(@QueryParam("providerUrl") String providerUrl, @Context HttpHeaders headers, @Suspended AsyncResponse response) {
        service.getByProviderUrl(providerUrl, extractAdditionalProperties(headers))
                .whenComplete((content, throwable) -> {
                    if (throwable == null) {
                        response.resume(content);
                    } else {
                        response.resume(throwable);
                    }
                });
    }

    private Map<String, Object> extractAdditionalProperties(HttpHeaders headers) {
        return headers.getRequestHeaders().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue()))
                .filter(entry -> entry.getKey().startsWith(EXTRA_PROPERTY_HEADER_PREFIX))
                .filter(entry -> entry.getValue().size() == 1)
                .collect(Collectors.toMap(entry ->
                        entry.getKey().replace(EXTRA_PROPERTY_HEADER_PREFIX, StringUtils.EMPTY),
                        entry -> entry.getValue().get(0)));
    }
}
