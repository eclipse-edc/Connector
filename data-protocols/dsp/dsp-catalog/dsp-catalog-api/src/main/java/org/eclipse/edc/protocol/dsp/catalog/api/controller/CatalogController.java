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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.catalog.spi.types.CatalogRequestMessage;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.eclipse.edc.jsonld.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class CatalogController {
    
    private ObjectMapper mapper;
    private IdentityService identityService;
    private JsonLdTransformerRegistry transformerRegistry;
    private String dspCallbackAddress;
    
    public CatalogController(ObjectMapper mapper, IdentityService identityService,
                             JsonLdTransformerRegistry transformerRegistry, String dspCallbackAddress) {
        this.mapper = mapper;
        this.identityService = identityService;
        this.transformerRegistry = transformerRegistry;
        this.dspCallbackAddress = dspCallbackAddress;
    }
    
    @POST
    @Path(CATALOG_REQUEST)
    public Map<String, Object> getCatalog(JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        var expanded = expand(jsonObject).getJsonObject(0); //expanding returns a JsonArray of size 1
        var transformResult = transformerRegistry.transform(expanded, CatalogRequestMessage.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException("Request body was malformed.");
        }

        //TODO use request and token to build catalog, replace empty catalog below
        var request = transformResult.getContent();
        var claimToken = readToken(token);
        var catalog = Json.createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DCAT_CATALOG_TYPE)
                .add(DCAT_DATASET_ATTRIBUTE, Json.createArrayBuilder().build())
                .add(DCAT_DATA_SERVICE_ATTRIBUTE, Json.createArrayBuilder().build())
                .build();
        
        return mapper.convertValue(compact(catalog, jsonLdContext()), Map.class);
    }
    
    private ClaimToken readToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var result = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress);
        if (result.failed()) {
            throw new AuthenticationFailedException();
        }

        return result.getContent();
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
