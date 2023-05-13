/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(BASE_PATH)
public class CatalogController {

    private final Monitor monitor;
    private final ObjectMapper mapper;
    private final IdentityService identityService;
    private final TypeTransformerRegistry transformerRegistry;
    private final String dspCallbackAddress;
    private final CatalogProtocolService service;
    private final JsonLd jsonLdService;

    public CatalogController(Monitor monitor, ObjectMapper mapper, IdentityService identityService,
                             TypeTransformerRegistry transformerRegistry, String dspCallbackAddress,
                             CatalogProtocolService service, JsonLd jsonLdService) {
        this.monitor = monitor;
        this.mapper = mapper;
        this.identityService = identityService;
        this.transformerRegistry = transformerRegistry;
        this.dspCallbackAddress = dspCallbackAddress;
        this.service = service;
        this.jsonLdService = jsonLdService;
    }

    @POST
    @Path(CATALOG_REQUEST)
    public Map<String, Object> getCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(() -> "DSP: Incoming catalog request.");

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var claimToken = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress)
                .orElseThrow(failure -> new AuthenticationFailedException());

        var expanded = jsonLdService.expand(jsonObject);
        if (expanded.failed()) {
            throw new InvalidRequestException(expanded.getFailureDetail());
        }
        var expandedJson = expanded.getContent();
        if (!isOfExpectedType(expandedJson, DSPACE_CATALOG_REQUEST_TYPE)) {
            throw new InvalidRequestException(format("Request body was not of expected type: %s", DSPACE_CATALOG_REQUEST_TYPE));
        }

        var message = transformerRegistry.transform(expandedJson, CatalogRequestMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        var catalog = service.getCatalog(message, claimToken)
                .orElseThrow(exceptionMapper(Catalog.class));

        var catalogJson = transformerRegistry.transform(catalog, JsonObject.class)
                .orElseThrow(failure -> new EdcException(format("Failed to build response: %s", failure.getFailureDetail())));

        var compacted = jsonLdService.compact(catalogJson);
        if (compacted.failed()) {
            throw new InvalidRequestException(compacted.getFailureDetail());
        }
        return mapper.convertValue(compacted.getContent(), Map.class);
    }

}
