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
import jakarta.json.Json;
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
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.util.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class CatalogController {
    
    private final ObjectMapper mapper;
    private final IdentityService identityService;
    private final JsonLdTransformerRegistry transformerRegistry;
    private final String dspCallbackAddress;
    private final CatalogProtocolService service;

    public CatalogController(ObjectMapper mapper, IdentityService identityService,
                             JsonLdTransformerRegistry transformerRegistry, String dspCallbackAddress,
                             CatalogProtocolService service) {
        this.mapper = mapper;
        this.identityService = identityService;
        this.transformerRegistry = transformerRegistry;
        this.dspCallbackAddress = dspCallbackAddress;
        this.service = service;
    }
    
    @POST
    @Path(CATALOG_REQUEST)
    public Map<String, Object> getCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var claimToken = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress)
                .orElseThrow(failure -> new AuthenticationFailedException());

        var expanded = expand(jsonObject).getJsonObject(0); //expanding returns a JsonArray of size 1
        if (!isOfExpectedType(expanded, DSPACE_CATALOG_REQUEST_TYPE)) {
            throw new InvalidRequestException(format("Request body was not of expected type: %s", DSPACE_CATALOG_REQUEST_TYPE));
        }
        
        var message = transformerRegistry.transform(expanded, CatalogRequestMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));
        
        var catalog = service.getCatalog(message, claimToken)
                .orElseThrow(exceptionMapper(Catalog.class));

        var catalogJson = transformerRegistry.transform(catalog, JsonObject.class)
                .orElseThrow(failure -> new EdcException(format("Failed to build response: %s", failure.getFailureDetail())));
        
        return mapper.convertValue(compact(catalogJson, jsonLdContext()), Map.class);
    }

    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DCAT_PREFIX, DCAT_SCHEMA)
                .add(DCT_PREFIX, DCT_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
