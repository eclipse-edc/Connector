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
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.api.configuration.error.DspErrorResponse;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;

/**
 * Provides the HTTP endpoint for receiving catalog requests.
 */
@Consumes({ APPLICATION_JSON })
@Produces({ APPLICATION_JSON })
@Path(BASE_PATH)
public class DspCatalogApiController {

    private final Monitor monitor;
    private final IdentityService identityService;
    private final TypeTransformerRegistry transformerRegistry;
    private final String dspCallbackAddress;
    private final CatalogProtocolService service;

    public DspCatalogApiController(Monitor monitor, IdentityService identityService,
                                   TypeTransformerRegistry transformerRegistry, String dspCallbackAddress,
                                   CatalogProtocolService service) {
        this.monitor = monitor;
        this.identityService = identityService;
        this.transformerRegistry = transformerRegistry;
        this.dspCallbackAddress = dspCallbackAddress;
        this.service = service;
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
            monitor.debug(format("Unauthorized, %s", verificationResult.getFailureMessages()));
            return error().unauthorized();
        }

        if (!isOfExpectedType(jsonObject, DSPACE_TYPE_CATALOG_REQUEST_MESSAGE)) {
            monitor.debug(format("Bad Request, %s", verificationResult.getFailureMessages()));
            return error().badRequest();
        }

        var transformResult = transformerRegistry.transform(jsonObject, CatalogRequestMessage.class);
        if (transformResult.failed()) {
            monitor.debug(format("Bad Request, %s", verificationResult.getFailureMessages()));
            return error().badRequest();
        }

        var message = transformResult.getContent();
        // set protocol
        message.setProtocol(DATASPACE_PROTOCOL_HTTP);

        var claimToken = verificationResult.getContent();

        var catalog = service.getCatalog(message, claimToken);
        if (catalog.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(format("Error returning catalog, error id %s: %s", errorCode, catalog.getFailureMessages()));
            return error().message(format("Error code %s", errorCode)).from(catalog.getFailure());
        }

        var catalogJson = transformerRegistry.transform(catalog.getContent(), JsonObject.class);
        if (catalogJson.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(format("Error transforming catalog, error id %s: %s", errorCode, catalogJson.getFailureMessages()));
            return error().message(format("Error code %s", errorCode)).internalServerError();
        }

        return status(Response.Status.OK)
                .type(APPLICATION_JSON)
                .entity(catalogJson.getContent())
                .build();
    }

    @NotNull
    private static DspErrorResponse error() {
        return DspErrorResponse.type(DSPACE_TYPE_CATALOG_ERROR);
    }

}
