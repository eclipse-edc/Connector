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
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.Namespaces;
import org.eclipse.edc.protocol.dsp.DspError;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.List;
import java.util.UUID;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.jsonld.spi.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.protocol.dsp.DspErrorDetails.BAD_REQUEST;
import static org.eclipse.edc.protocol.dsp.DspErrorDetails.UNAUTHORIZED;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(BASE_PATH)
public class DspCatalogApiController {

    private static final String DSPACE_CATALOG_ERROR = Namespaces.DSPACE_SCHEMA + "CatalogError"; // TODO move to :dsp-core https://github.com/eclipse-edc/Connector/issues/3014

    private final Monitor monitor;
    private final IdentityService identityService;
    private final TypeTransformerRegistry transformerRegistry;
    private final String dspCallbackAddress;
    private final CatalogProtocolService service;
    private final JsonLd jsonLdService;

    public DspCatalogApiController(Monitor monitor, IdentityService identityService,
                                   TypeTransformerRegistry transformerRegistry, String dspCallbackAddress,
                                   CatalogProtocolService service, JsonLd jsonLdService) {
        this.monitor = monitor;
        this.identityService = identityService;
        this.transformerRegistry = transformerRegistry;
        this.dspCallbackAddress = dspCallbackAddress;
        this.service = service;
        this.jsonLdService = jsonLdService;
    }

    @POST
    @Path(CATALOG_REQUEST)
    public Response getCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(() -> "DSP: Incoming catalog request.");

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var verificationResult = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress);
        if (verificationResult.failed()) {
            monitor.debug(String.format("%s, %s", UNAUTHORIZED, verificationResult.getFailureMessages()));
            return errorResponse(Response.Status.UNAUTHORIZED, UNAUTHORIZED);
        }

        var expanded = jsonLdService.expand(jsonObject);
        if (expanded.failed()) {
            monitor.debug(String.format("%s, %s", BAD_REQUEST, verificationResult.getFailureMessages()));
            return errorResponse(Response.Status.BAD_REQUEST, BAD_REQUEST);
        }

        var expandedJson = expanded.getContent();
        if (!isOfExpectedType(expandedJson, DSPACE_CATALOG_REQUEST_TYPE)) {
            monitor.debug(String.format("%s, %s", BAD_REQUEST, verificationResult.getFailureMessages()));
            return errorResponse(Response.Status.BAD_REQUEST, BAD_REQUEST);
        }

        var transformResult = transformerRegistry.transform(expandedJson, CatalogRequestMessage.class);
        if (transformResult.failed()) {
            monitor.debug(String.format("%s, %s", BAD_REQUEST, verificationResult.getFailureMessages()));
            return errorResponse(Response.Status.BAD_REQUEST, BAD_REQUEST);
        }

        var message = transformResult.getContent();
        // set protocol
        message.setProtocol(DATASPACE_PROTOCOL_HTTP);

        var claimToken = verificationResult.getContent();

        var catalog = service.getCatalog(message, claimToken);
        if (catalog.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(String.format("Error returning catalog, error id %s: %s", errorCode, catalog.getFailureMessages()));
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, String.format("Error code %s", errorCode));
        }

        var catalogJson = transformerRegistry.transform(catalog.getContent(), JsonObject.class);
        if (catalogJson.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(String.format("Error transforming catalog, error id %s: %s", errorCode, catalogJson.getFailureMessages()));
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, String.format("Error code %s", errorCode));
        }

        var compacted = jsonLdService.compact(catalogJson.getContent());
        if (compacted.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(String.format("Error compacting catalog, error id %s: %s", errorCode, catalogJson.getFailureMessages()));
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, String.format("Error code %s", errorCode));
        }

        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(compacted.getContent()).build();
    }

    private Response errorResponse(Response.Status code, String message) {
        return Response.status(code).type(MediaType.APPLICATION_JSON)
                .entity(DspError.Builder.newInstance()
                        .type(DSPACE_CATALOG_ERROR)
                        .code(Integer.toString(code.getStatusCode()))
                        .messages(List.of(message))
                        .build().toJson())
                .build();
    }

}
