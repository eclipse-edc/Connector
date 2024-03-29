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

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.spi.message.PostDspRequest;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({ APPLICATION_JSON })
@Produces({ APPLICATION_JSON })
@Path(BASE_PATH)
public class DspCatalogApiController {

    private final CatalogProtocolService service;
    private final DspRequestHandler dspRequestHandler;

    public DspCatalogApiController(CatalogProtocolService service, DspRequestHandler dspRequestHandler) {
        this.service = service;
        this.dspRequestHandler = dspRequestHandler;
    }

    @POST
    @Path(CATALOG_REQUEST)
    public Response requestCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(CatalogRequestMessage.class, Catalog.class)
                .token(token)
                .expectedMessageType(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE)
                .message(jsonObject)
                .serviceCall(service::getCatalog)
                .errorType(DSPACE_TYPE_CATALOG_ERROR)
                .build();

        return dspRequestHandler.createResource(request);
    }

    @GET
    @Path(DATASET_REQUEST + "/{id}")
    public Response getDataset(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        var request = GetDspRequest.Builder.newInstance(Dataset.class)
                .token(token)
                .id(id)
                .serviceCall(service::getDataset)
                .errorType(DSPACE_TYPE_CATALOG_ERROR)
                .build();

        return dspRequestHandler.getResource(request);
    }

}
