/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api.signaling;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;

import java.util.Objects;

@Provider
public class DataPlaneTransferAuthorizationFilter implements ContainerRequestFilter {

    private final SignalingAuthorizationRegistry signalingAuthorizationRegistry;
    private final TransferProcessService transferProcessService;
    private final DataPlaneSelectorService dataPlaneSelectorService;

    public DataPlaneTransferAuthorizationFilter(SignalingAuthorizationRegistry signalingAuthorizationRegistry,
                                                TransferProcessService transferProcessService,
                                                DataPlaneSelectorService dataPlaneSelectorService) {
        this.signalingAuthorizationRegistry = signalingAuthorizationRegistry;
        this.transferProcessService = transferProcessService;
        this.dataPlaneSelectorService = dataPlaneSelectorService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var transferId = extractTransferId(requestContext);

        var transferProcess = transferProcessService.findById(transferId);
        if (transferProcess == null) {
            throw new NotAuthorizedException("Not authorized");
        }

        var dataPlaneId = transferProcess.getDataPlaneId();
        var dataPlane = dataPlaneSelectorService.findById(dataPlaneId)
                .orElseThrow(f -> new NotAuthorizedException("Not authorized"));

        var authorizationProfile = dataPlane.getAuthorizationProfile();
        if (authorizationProfile == null) {
            return;
        }

        var authorization = signalingAuthorizationRegistry.findByType(authorizationProfile.type());
        if (authorization == null) {
            throw new InternalServerErrorException("DataPlane %s has an AuthorizationProfile of type %s, that's not registered correctly"
                    .formatted(dataPlaneId, authorizationProfile.type()));
        }

        var callerDataPlaneId = authorization.isAuthorized(requestContext::getHeaderString, authorizationProfile)
                .orElseThrow(f -> new NotAuthorizedException("Not authorized"));

        if (!Objects.equals(dataPlaneId, callerDataPlaneId)) {
            throw new NotAuthorizedException("Not authorized");
        }
    }

    private String extractTransferId(ContainerRequestContext requestContext) {
        var transferIds = requestContext.getUriInfo().getPathParameters().get("transferId");
        if (transferIds == null || transferIds.isEmpty()) {
            throw new InternalServerErrorException("Cannot extract 'transferId' from URI info");
        }

        return transferIds.get(0);
    }
}
