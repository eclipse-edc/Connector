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

package org.eclipse.edc.protocol.dsp.catalog.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.protocol.dsp.spi.catalog.service.CatalogService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.api.configuration.auth.ClaimTokenRequestFilter.CLAIM_TOKEN_HEADER;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.expandDocument;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/catalog")
public class CatalogController {
    
    private Monitor monitor;
    private CatalogService catalogService;
    private ObjectMapper mapper;
    
    public CatalogController(Monitor monitor, CatalogService catalogService, ObjectMapper mapper) {
        this.monitor = monitor;
        this.catalogService = catalogService;
        this.mapper = mapper;
    }
    
    @POST
    @Path("/request")
    public Map<String, Object> getCatalog(JsonObject jsonObject, @HeaderParam(CLAIM_TOKEN_HEADER) String token) {
        var document = expandDocument(jsonObject); //expanding document returns a JsonArray of size 1
        
        var catalog = catalogService.getCatalog(document.getJsonObject(0), readToken(token));
    
        return mapper.convertValue(compactDocument(catalog), Map.class);
    }
    
    private ClaimToken readToken(String token) {
        try {
            return mapper.readValue(token, ClaimToken.class);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to read request.");
        }
    }
}
