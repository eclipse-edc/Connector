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

package org.eclipse.edc.protocol.dsp.http;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.DEACTIVATED;

/**
 * JAX-RS resource that dispatches requests to sub-resources of a DSP virtual profile resource.
 * <p>
 * The sub-resources are identified by the last path segment, which must be one of "catalog", "negotiations", or "transfers".
 * The actual sub-resource instance is obtained from the {@link DspVirtualSubResourceLocator}, which allows to decouple the
 * dispatcher from the sub-resources and to register them in a flexible way.
 * <p>
 * A deactivated participant context is answered like one that does not exist.
 */
@Path("/{participantContextId}/{profileId}/{subResource:catalog|negotiations|transfers}")
public class DspVirtualProfileDispatcher {

    private final ParticipantProfileService profileResolver;
    private final DspVirtualSubResourceLocator subResourceLocator;
    private final ParticipantContextService participantContextService;

    public DspVirtualProfileDispatcher(ParticipantProfileService profileResolver, DspVirtualSubResourceLocator subResourceLocator,
                                       ParticipantContextService participantContextService) {
        this.profileResolver = profileResolver;
        this.subResourceLocator = subResourceLocator;
        this.participantContextService = participantContextService;
    }

    @Path("/")
    public Object dispatch(@PathParam("participantContextId") String participantId,
                           @PathParam("profileId") String profileId, @PathParam("subResource") String subResource) {

        rejectDeactivated(participantId);
        var profile = resolveProfile(participantId, profileId);
        var subResourceController = subResourceLocator.getSubResource(subResource, profile.protocolVersion().version());
        if (subResourceController == null) throw new NotFoundException();
        return subResourceController;
    }

    private void rejectDeactivated(String participantContextId) {
        var participantContext = participantContextService.getParticipantContext(participantContextId);
        if (participantContext.succeeded() && participantContext.getContent().getStateAsEnum() == DEACTIVATED) {
            throw new NotFoundException("No participant '%s'".formatted(participantContextId));
        }
    }

    private DataspaceProfileContext resolveProfile(String participantContextId, String profileId) {
        return ofNullable(profileResolver.resolve(participantContextId, profileId))
                .orElseThrow(() -> new NotFoundException("No profile '%s' for participant '%s'".formatted(profileId, participantContextId)));
    }
}
