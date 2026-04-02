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
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferAuthorizationFilterTest {

    private final SignalingAuthorizationRegistry registry = mock();
    private final ContainerRequestContext requestContext = mock();
    private final DataPlaneTransferAuthorizationFilter filter = new DataPlaneTransferAuthorizationFilter(registry);

    @Test
    void filter_shouldPassThrough_whenNoAuthorizationsRegistered() {
        when(registry.getAll()).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> filter.filter(requestContext));

        verify(requestContext, never()).setProperty(any(), any());
    }

    @Test
    void filter_shouldSetDataPlaneId_whenAuthorizationSucceeds() {
        var dataPlaneId = "data-plane-1";
        var authorization = authorizationThatReturns(Result.success(dataPlaneId));
        when(registry.getAll()).thenReturn(List.of(authorization));

        filter.filter(requestContext);

        verify(requestContext).setProperty(eq(DataPlaneTransferAuthorizationFilter.DATA_PLANE_ID), eq(dataPlaneId));
    }

    @Test
    void filter_shouldThrowNotAuthorized_whenAllAuthorizationsFail() {
        var auth1 = authorizationThatReturns(Result.failure("invalid token"));
        var auth2 = authorizationThatReturns(Result.failure("missing header"));
        when(registry.getAll()).thenReturn(List.of(auth1, auth2));

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class);

        verify(requestContext, never()).setProperty(any(), any());
    }

    @Test
    void filter_shouldSetDataPlaneId_whenOneOfMultipleAuthorizationsSucceeds() {
        var dataPlaneId = "data-plane-2";
        var failing = authorizationThatReturns(Result.failure("invalid token"));
        var succeeding = authorizationThatReturns(Result.success(dataPlaneId));
        when(registry.getAll()).thenReturn(List.of(failing, succeeding));

        filter.filter(requestContext);

        verify(requestContext).setProperty(eq(DataPlaneTransferAuthorizationFilter.DATA_PLANE_ID), eq(dataPlaneId));
    }

    private SignalingAuthorization authorizationThatReturns(Result<String> result) {
        var auth = mock(SignalingAuthorization.class);
        when(auth.isAuthorized(any())).thenReturn(result);
        return auth;
    }
}
