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
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;

/**
 * JAX-RS resource that dispatches requests to sub-resources of a DSP virtual profile resource.
 * <p>
 * The sub-resources are identified by the last path segment, which must be one of "catalog", "negotiations", or "transfers".
 * The actual sub-resource instance is obtained from the {@link DspVirtualSubResourceLocator}, which allows to decouple the
 * dispatcher from the sub-resources and to register them in a flexible way.
 */
@Path("/{participantContextId}/{profileId}/{subResource:catalog|negotiations|transfers}")
public class DspVirtualProfileDispatcher {

    private final ParticipantProfileResolver profileResolver;
    private final DspVirtualSubResourceLocator subResourceLocator;

    public DspVirtualProfileDispatcher(ParticipantProfileResolver profileResolver, DspVirtualSubResourceLocator subResourceLocator) {
        this.profileResolver = profileResolver;
        this.subResourceLocator = subResourceLocator;
    }

    @Path("/")
    public Object dispatch(@PathParam("participantContextId") String participantId,
                           @PathParam("profileId") String profileId, @PathParam("subResource") String subResource) {

        var profile = resolveProfile(participantId, profileId);
        var subResourceController = subResourceLocator.getSubResource(subResource, profile.protocolVersion().version());
        if (subResourceController == null) throw new NotFoundException();
        return subResourceController;
    }

    private DataspaceProfileContext resolveProfile(String participantContextId, String profileId) {
        return profileResolver.resolve(participantContextId, profileId)
                .orElseThrow(() -> new NotFoundException("No profile '%s' for participant '%s'".formatted(profileId, participantContextId)));
    }
}
