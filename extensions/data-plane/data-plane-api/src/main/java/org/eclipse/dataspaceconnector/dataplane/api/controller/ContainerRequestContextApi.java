/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Daimler TSS GmbH - security improvement: don't overwrite values of DataAddress
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides a set of API wrapping a {@link ContainerRequestContext}.
 */
public interface ContainerRequestContextApi {
    /**
     * Return the value of the auth header provided in the request.
     */
    @Nullable
    String authHeader(ContainerRequestContext context);

    String queryParams(ContainerRequestContext context);

    String body(ContainerRequestContext context);

    String path(ContainerRequestContext context);

    String mediaType(ContainerRequestContext context);

    String method(ContainerRequestContext context);
}
