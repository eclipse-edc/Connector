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

package org.eclipse.edc.protocol.dsp.catalog.http.api.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogError;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_IRI;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({ APPLICATION_JSON })
@Produces({ APPLICATION_JSON })
@Path(BASE_PATH)
public class DspCatalogApiController {

    private final CatalogProtocolService service;
    private final DspRequestHandler dspRequestHandler;
    private final ContinuationTokenManager continuationTokenManager;
    private final String protocol;

    public DspCatalogApiController(CatalogProtocolService service, DspRequestHandler dspRequestHandler, ContinuationTokenManager continuationTokenManager) {
        this(service, dspRequestHandler, continuationTokenManager, DATASPACE_PROTOCOL_HTTP);
    }

    public DspCatalogApiController(CatalogProtocolService service, DspRequestHandler dspRequestHandler, ContinuationTokenManager continuationTokenManager, String protocol) {
        this.service = service;
        this.dspRequestHandler = dspRequestHandler;
        this.continuationTokenManager = continuationTokenManager;
        this.protocol = protocol;
    }

    @POST
    @Path(CATALOG_REQUEST)
    public Response requestCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token, @Context UriInfo uriInfo,
                                   @QueryParam("continuationToken") String continuationToken) {
        JsonObject messageJson;
        if (continuationToken == null) {
            messageJson = jsonObject;
        } else {
            messageJson = continuationTokenManager.applyQueryFromToken(jsonObject, continuationToken)
                    .orElseThrow(f -> new BadRequestException(f.getFailureDetail()));
        }

        var request = PostDspRequest.Builder.newInstance(CatalogRequestMessage.class, Catalog.class, CatalogError.class)
                .token(token)
                .expectedMessageType(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_IRI)
                .message(messageJson)
                .serviceCall(service::getCatalog)
                .errorProvider(CatalogError.Builder::newInstance)
                .protocol(protocol)
                .build();

        var responseDecorator = continuationTokenManager.createResponseDecorator(uriInfo.getAbsolutePath().toString());
        return dspRequestHandler.createResource(request, responseDecorator);
    }

    @GET
    @Path(DATASET_REQUEST + "/{id}")
    public Response getDataset(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        var request = GetDspRequest.Builder.newInstance(Dataset.class, CatalogError.class)
                .token(token)
                .id(id)
                .serviceCall(service::getDataset)
                .errorProvider(CatalogError.Builder::newInstance)
                .protocol(protocol)
                .build();

        return dspRequestHandler.getResource(request);
    }

}
