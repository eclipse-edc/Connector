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

package org.eclipse.edc.signaling.port.api;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.Result;

@Provider
public class DataPlaneTransferAuthorizationFilter implements ContainerRequestFilter {

    public static final String DATA_PLANE_ID = "dataPlaneId";
    private final SignalingAuthorizationRegistry signalingAuthorizationRegistry;

    public DataPlaneTransferAuthorizationFilter(SignalingAuthorizationRegistry signalingAuthorizationRegistry) {
        this.signalingAuthorizationRegistry = signalingAuthorizationRegistry;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var authorizations = signalingAuthorizationRegistry.getAll();
        if (authorizations.isEmpty()) {
            return;
        }

        var dataPlaneId = authorizations.stream()
                .map(authorization -> authorization.isAuthorized(requestContext::getHeaderString))
                .filter(Result::succeeded)
                .findAny()
                .orElseThrow(() -> new NotAuthorizedException("Not authorized"))
                .orElseThrow(failure -> new NotAuthorizedException(failure.getFailureDetail()));

        requestContext.setProperty(DATA_PLANE_ID, dataPlaneId);
    }
}
