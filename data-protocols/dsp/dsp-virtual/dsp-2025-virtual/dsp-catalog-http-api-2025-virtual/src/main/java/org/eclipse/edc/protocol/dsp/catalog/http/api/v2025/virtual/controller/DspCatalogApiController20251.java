/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api.v2025.virtual.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
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
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;

/**
 * Versioned Catalog endpoint for 2025/1 protocol version. Path is scoped by participant context
 * id, profile id and DSP protocol version segment. The version segment dispatches to this
 * controller class; the profile determines the JSON-LD namespace and protocol string used to
 * dispatch the request. The profile's DSP version must match this controller's version.
 * The controller is not actually registered directly on the web service, but is returned by the DspVirtualProfileDispatcher when
 * a request matches the path parameters and version. This allows for multiple versions of the same API to be supported simultaneously.
 */
@Consumes({APPLICATION_JSON})
@Produces({APPLICATION_JSON})
@Path("/{participantContextId}/{profileId}" + BASE_PATH)
public class DspCatalogApiController20251 {

    private final CatalogProtocolService service;
    private final ParticipantContextService participantContextService;
    private final ParticipantProfileResolver profileResolver;
    private final DspRequestHandler dspRequestHandler;
    private final ContinuationTokenManager continuationTokenManager;

    public DspCatalogApiController20251(CatalogProtocolService service,
                                        ParticipantContextService participantContextService,
                                        ParticipantProfileResolver profileResolver,
                                        DspRequestHandler dspRequestHandler,
                                        ContinuationTokenManager continuationTokenManager) {
        this.service = service;
        this.participantContextService = participantContextService;
        this.profileResolver = profileResolver;
        this.dspRequestHandler = dspRequestHandler;
        this.continuationTokenManager = continuationTokenManager;
    }

    @POST
    @Path(CATALOG_REQUEST)
    public Response requestCatalog(@PathParam("participantContextId") String participantContextId,
                                   @PathParam("profileId") String profileId,
                                   JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token, @Context UriInfo uriInfo,
                                   @QueryParam("continuationToken") String continuationToken) {
        var profile = resolveProfile(participantContextId, profileId);
        JsonObject messageJson;
        if (continuationToken == null) {
            messageJson = jsonObject;
        } else {
            messageJson = continuationTokenManager.applyQueryFromToken(jsonObject, continuationToken)
                    .orElseThrow(f -> new BadRequestException(f.getFailureDetail()));
        }

        var request = PostDspRequest.Builder.newInstance(CatalogRequestMessage.class, Catalog.class, CatalogError.class)
                .token(token)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM))
                .message(messageJson)
                .serviceCall(service::getCatalog)
                .errorProvider(CatalogError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        var responseDecorator = continuationTokenManager.createResponseDecorator(uriInfo.getAbsolutePath().toString());
        return dspRequestHandler.createResource(request, responseDecorator);
    }

    @GET
    @Path(DATASET_REQUEST + "/{id}")
    public Response getDataset(@PathParam("participantContextId") String participantContextId,
                               @PathParam("profileId") String profileId,
                               @PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var message = DatasetRequestMessage.Builder.newInstance()
                .datasetId(id)
                .protocol(profile.name())
                .build();

        var request = GetDspRequest.Builder.newInstance(DatasetRequestMessage.class, Dataset.class, CatalogError.class)
                .token(token)
                .message(message)
                .id(id)
                .serviceCall(service::getDataset)
                .errorProvider(CatalogError.Builder::newInstance)
                .participantContextProvider(participantContextSupplier(participantContextId))
                .protocol(profile.name())
                .build();

        return dspRequestHandler.getResource(request);
    }

    private DataspaceProfileContext resolveProfile(String participantContextId, String profileId) {
        var profile = profileResolver.resolve(participantContextId, profileId)
                .orElseThrow(() -> new NotFoundException("No profile '%s' for participant '%s'".formatted(profileId, participantContextId)));
        if (!V_2025_1_VERSION.equals(profile.protocolVersion().version())) {
            throw new NotFoundException("Profile '%s' is not for DSP version %s".formatted(profileId, V_2025_1_VERSION));
        }
        return profile;
    }

    private ParticipantContextSupplier participantContextSupplier(String id) {
        return () -> participantContextService.getParticipantContext(id);
    }
}
