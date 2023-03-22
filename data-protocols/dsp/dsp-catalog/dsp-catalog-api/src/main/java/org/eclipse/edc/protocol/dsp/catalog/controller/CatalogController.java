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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.protocol.dsp.spi.catalog.service.CatalogService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.util.JsonLdUtil.compactDocument;
import static org.eclipse.edc.protocol.dsp.util.JsonLdUtil.expandDocument;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/catalog")
public class CatalogController {

    private Monitor monitor;
    private CatalogService catalogService;
    private ObjectMapper mapper;

    public CatalogController(Monitor monitor, CatalogService catalogService, TypeManager typeManager) {
        this.monitor = monitor;
        this.catalogService = catalogService;
        this.mapper = typeManager.getMapper("json-ld");
    }

    //TODO authentication

    @POST
    @Path("/request")
    public Map<String, Object> getCatalog(JsonObject jsonObject) {
        var document = expandDocument(jsonObject);

        var catalog = catalogService.getCatalog(document.getJsonObject(0)); //expanding document returns a JsonArray of size 1

        return mapper.convertValue(compactDocument(catalog), Map.class);
    }
}
